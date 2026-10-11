package com.habittracker.service;

import com.habittracker.dto.HabitRequest;
import com.habittracker.dto.HabitResponse;
import com.habittracker.dto.HabitStats;
import com.habittracker.exception.ResourceNotFoundException;
import com.habittracker.model.Habit;
import com.habittracker.model.HabitLog;
import com.habittracker.repository.HabitLogRepository;
import com.habittracker.repository.HabitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class HabitService {

    private static final String DEFAULT_COLOR = "#4CAF50";

    private final HabitRepository habitRepository;
    private final HabitLogRepository logRepository;

    public HabitService(HabitRepository habitRepository, HabitLogRepository logRepository) {
        this.habitRepository = habitRepository;
        this.logRepository = logRepository;
    }

    // ---------- CRUD ----------

    public List<HabitResponse> getAllHabits(String ownerId) {
        List<Habit> list = habitRepository.findByOwnerIdOrderByIdDesc(ownerId);
        if (list.isEmpty()) return List.of();
        // one query for every check-in, instead of one query per habit
        Map<Long, TreeSet<LocalDate>> byHabit = new HashMap<>();
        for (HabitLog log : logRepository.findByHabitIdIn(list.stream().map(Habit::getId).toList())) {
            byHabit.computeIfAbsent(log.getHabit().getId(), k -> new TreeSet<>()).add(log.getLogDate());
        }
        return list.stream()
                .map(h -> toResponse(h, byHabit.getOrDefault(h.getId(), new TreeSet<>())))
                .toList();
    }

    public HabitResponse getHabit(String ownerId, Long id) {
        return toResponse(findHabit(ownerId, id));
    }

    @Transactional
    public HabitResponse createHabit(String ownerId, HabitRequest request) {
        Habit habit = new Habit();
        habit.setOwnerId(ownerId);
        apply(habit, request);
        return toResponse(habitRepository.save(habit));
    }

    @Transactional
    public HabitResponse updateHabit(String ownerId, Long id, HabitRequest request) {
        Habit habit = findHabit(ownerId, id);
        apply(habit, request);
        return toResponse(habitRepository.save(habit));
    }

    @Transactional
    public void deleteHabit(String ownerId, Long id) {
        Habit habit = findHabit(ownerId, id);
        logRepository.deleteByHabitId(id);
        habitRepository.delete(habit);
    }

    // ---------- Check-ins ----------

    /** Marks the habit done for the date, or un-marks it if it was already done. */
    @Transactional
    public HabitResponse toggleCompletion(String ownerId, Long id, LocalDate date) {
        Habit habit = findHabit(ownerId, id);
        LocalDate target = (date != null) ? date : LocalDate.now();

        if (target.isAfter(LocalDate.now().plusDays(1))) {
            throw new IllegalArgumentException("Cannot mark a habit for a future date");
        }
        if (target.isBefore(habit.getCreatedDate())) {
            throw new IllegalArgumentException("Cannot mark a habit before its start date");
        }
        if (!habit.getScheduledDates().isEmpty() && !habit.getScheduledDates().contains(target)) {
            throw new IllegalArgumentException("This habit is not scheduled for that day");
        }

        Optional<HabitLog> existing = logRepository.findByHabitIdAndLogDate(id, target);
        if (existing.isPresent()) {
            logRepository.delete(existing.get());
        } else {
            logRepository.save(new HabitLog(habit, target));
        }
        return toResponse(habit);
    }

    /** Completed dates in a range (used to draw the calendar / heatmap). */
    public List<LocalDate> getCompletedDates(String ownerId, Long id, LocalDate from, LocalDate to) {
        findHabit(ownerId, id);
        LocalDate end = (to != null) ? to : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(364);
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }
        return logRepository.findByHabitIdAndLogDateBetweenOrderByLogDateAsc(id, start, end)
                .stream().map(HabitLog::getLogDate).toList();
    }

    // ---------- Stats ----------

    public HabitStats getStats(String ownerId, Long id) {
        Habit habit = findHabit(ownerId, id);
        TreeSet<LocalDate> dates = loadDates(id);
        LocalDate today = LocalDate.now();

        if (!habit.getScheduledDates().isEmpty()) {
            return scheduledStats(habit, dates, today);
        }

        long last30 = dates.stream()
                .filter(d -> !d.isBefore(today.minusDays(29)))
                .count();
        long windowDays = Math.min(30, ChronoUnit.DAYS.between(habit.getCreatedDate(), today) + 1);
        double rate = windowDays <= 0 ? 0 : Math.round((last30 * 1000.0) / windowDays) / 10.0;

        return new HabitStats(id, currentStreak(dates), longestStreak(dates), dates.size(), rate);
    }

    // ---------- Helpers ----------

    private Habit findHabit(String ownerId, Long id) {
        return habitRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id " + id));
    }

    private void apply(Habit habit, HabitRequest request) {
        habit.setName(request.name().trim());
        habit.setDescription(request.description());
        habit.setColor(request.color() != null ? request.color() : DEFAULT_COLOR);
        String reminder = request.reminderTime();
        habit.setReminderTime(reminder == null || reminder.isBlank() ? null : reminder);
        if (request.scheduledDates() != null) {
            habit.getScheduledDates().clear();
            habit.getScheduledDates().addAll(request.scheduledDates());
        }
        if (!habit.getScheduledDates().isEmpty()) {
            // a habit with chosen dates "starts" on its earliest date
            habit.setCreatedDate(Collections.min(habit.getScheduledDates()));
        } else if (request.startDate() != null) {
            habit.setCreatedDate(request.startDate());
        }
    }

    private TreeSet<LocalDate> loadDates(Long habitId) {
        return logRepository.findByHabitIdOrderByLogDateAsc(habitId).stream()
                .map(HabitLog::getLogDate)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private HabitResponse toResponse(Habit habit) {
        return toResponse(habit, loadDates(habit.getId()));
    }

    private HabitResponse toResponse(Habit habit, TreeSet<LocalDate> dates) {
        return new HabitResponse(
                habit.getId(),
                habit.getName(),
                habit.getDescription(),
                habit.getColor(),
                habit.getCreatedDate(),
                habit.getReminderTime(),
                habit.getScheduledDates().stream().sorted().toList(),
                dates.contains(LocalDate.now()),
                habit.getScheduledDates().isEmpty()
                        ? currentStreak(dates)
                        : currentStreakScheduled(dates, habit.getScheduledDates()));
    }

    // ----- habits with specific scheduled dates: streaks count scheduled days only -----

    private int currentStreakScheduled(Set<LocalDate> done, Set<LocalDate> schedule) {
        LocalDate today = LocalDate.now();
        int streak = 0;
        for (LocalDate d : new TreeSet<>(schedule).headSet(today, true).descendingSet()) {
            if (done.contains(d)) {
                streak++;
            } else if (!d.equals(today)) {
                break; // a missed scheduled day ends the streak; today still pending does not
            }
        }
        return streak;
    }

    private HabitStats scheduledStats(Habit habit, Set<LocalDate> done, LocalDate today) {
        TreeSet<LocalDate> schedule = new TreeSet<>(habit.getScheduledDates());
        int longest = 0, run = 0;
        for (LocalDate d : schedule.headSet(today, true)) {
            if (done.contains(d)) {
                run++;
                longest = Math.max(longest, run);
            } else if (!d.equals(today)) {
                run = 0;
            }
        }
        LocalDate from = today.minusDays(29);
        long planned = schedule.subSet(from, true, today, true).size();
        long completed = schedule.subSet(from, true, today, true).stream().filter(done::contains).count();
        double rate = planned == 0 ? 0 : Math.round((completed * 1000.0) / planned) / 10.0;
        return new HabitStats(habit.getId(), currentStreakScheduled(done, schedule), longest, done.size(), rate);
    }

    /** Consecutive days ending today (or yesterday, so the streak survives until the day ends). */
    private int currentStreak(Set<LocalDate> dates) {
        LocalDate day = LocalDate.now();
        if (!dates.contains(day)) {
            day = day.minusDays(1);
        }
        int streak = 0;
        while (dates.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }

    private int longestStreak(TreeSet<LocalDate> dates) {
        int longest = 0, run = 0;
        LocalDate prev = null;
        for (LocalDate d : dates) {
            run = (prev != null && prev.plusDays(1).equals(d)) ? run + 1 : 1;
            longest = Math.max(longest, run);
            prev = d;
        }
        return longest;
    }
}
