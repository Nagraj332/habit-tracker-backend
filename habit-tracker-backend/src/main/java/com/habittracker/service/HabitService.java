package com.habittracker.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.habittracker.dto.HabitRequest;
import com.habittracker.dto.HabitResponse;
import com.habittracker.dto.StatsResponse;
import com.habittracker.exception.BadRequestException;
import com.habittracker.exception.NotFoundException;
import com.habittracker.model.Habit;
import com.habittracker.model.HabitLog;
import com.habittracker.repository.HabitLogRepository;
import com.habittracker.repository.HabitRepository;
import com.habittracker.repository.WalkRepository;

@Service
public class HabitService {

    private static final String DEFAULT_COLOR = "#0f9d8a";
    private static final long MAX_LOG_RANGE_DAYS = 1100;

    private final HabitRepository habits;
    private final HabitLogRepository logs;
    private final WalkRepository walks;
    private final ZoneId zone;

    public HabitService(HabitRepository habits, HabitLogRepository logs, WalkRepository walks, ZoneId zone) {
        this.habits = habits;
        this.logs = logs;
        this.walks = walks;
        this.zone = zone;
    }

    private LocalDate today() {
        return LocalDate.now(zone);
    }

    // ---------- reads ----------

    @Transactional(readOnly = true)
    public List<HabitResponse> list(String owner) {
        List<Habit> all = habits.findByOwnerIdOrderByIdAsc(owner);
        if (all.isEmpty()) return List.of();
        Map<Long, Set<LocalDate>> done = doneByHabit(all.stream().map(Habit::getId).toList());
        LocalDate today = today();
        return all.stream()
                .map(h -> toResponse(h, done.getOrDefault(h.getId(), Set.of()), today))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LocalDate> logs(String owner, Long id, LocalDate from, LocalDate to) {
        find(owner, id);
        if (to.isBefore(from)) throw new BadRequestException("'to' must not be before 'from'.");
        if (ChronoUnit.DAYS.between(from, to) > MAX_LOG_RANGE_DAYS) {
            throw new BadRequestException("That date range is too long.");
        }
        return logs.findDatesBetween(id, from, to);
    }

    @Transactional(readOnly = true)
    public StatsResponse stats(String owner, Long id) {
        Habit h = find(owner, id);
        Set<LocalDate> done = new HashSet<>(logs.findDatesByHabitId(id));
        return HabitStats.compute(h.getCreatedDate(), h.getScheduledDates(), done, today());
    }

    // ---------- writes ----------

    @Transactional
    public HabitResponse create(String owner, HabitRequest req) {
        Habit h = new Habit();
        h.setOwnerId(owner);
        apply(h, req);
        habits.save(h);
        return respond(h);
    }

    @Transactional
    public HabitResponse update(String owner, Long id, HabitRequest req) {
        Habit h = find(owner, id);
        apply(h, req);
        habits.save(h);
        return respond(h);
    }

    @Transactional
    public void delete(String owner, Long id) {
        Habit h = find(owner, id);
        logs.deleteAllByHabit(id);
        walks.detachHabit(id);
        habits.delete(h);
    }

    /** Checks the habit off for a day, or unchecks it if it was already done. Returns the updated habit. */
    @Transactional
    public HabitResponse toggle(String owner, Long id, LocalDate date) {
        Habit h = find(owner, id);
        LocalDate today = today();
        LocalDate day = date == null ? today : date;
        // one day of slack, because the phone's "today" can be ahead of the server's
        if (day.isAfter(today.plusDays(1))) throw new BadRequestException("You can't check in on a future date.");
        Optional<HabitLog> existing = logs.findByHabitIdAndLogDate(id, day);
        if (existing.isPresent()) logs.delete(existing.get());
        else logs.save(new HabitLog(id, day));
        return respond(h);
    }

    // ---------- helpers ----------

    private Habit find(String owner, Long id) {
        return habits.findByIdAndOwnerId(id, owner).orElseThrow(() -> new NotFoundException("Habit not found."));
    }

    private void apply(Habit h, HabitRequest r) {
        h.setName(r.name().trim());
        h.setDescription(blankToNull(r.description()));
        h.setColor(blankToNull(r.color()) == null ? DEFAULT_COLOR : r.color().trim());
        h.setReminderTime(blankToNull(r.reminderTime()));

        Set<LocalDate> dates = new TreeSet<>();
        if (r.scheduledDates() != null) {
            r.scheduledDates().stream().filter(Objects::nonNull).forEach(dates::add);
        }
        h.getScheduledDates().clear();
        h.getScheduledDates().addAll(dates);

        if (dates.isEmpty() && r.startDate() != null) {
            h.setCreatedDate(r.startDate()); // a daily habit starts on its start date
        } else if (h.getCreatedDate() == null) {
            h.setCreatedDate(today());
        }
    }

    private HabitResponse respond(Habit h) {
        Set<LocalDate> done = new HashSet<>(logs.findDatesByHabitId(h.getId()));
        return toResponse(h, done, today());
    }

    private HabitResponse toResponse(Habit h, Set<LocalDate> done, LocalDate today) {
        Set<LocalDate> scheduled = h.getScheduledDates();
        return new HabitResponse(
                h.getId(),
                h.getName(),
                h.getDescription(),
                h.getColor(),
                h.getReminderTime(),
                scheduled.isEmpty() ? h.getCreatedDate() : null,
                h.getCreatedDate(),
                List.copyOf(new TreeSet<>(scheduled)),
                done.contains(today),
                HabitStats.currentStreak(h.getCreatedDate(), scheduled, done, today));
    }

    private Map<Long, Set<LocalDate>> doneByHabit(Collection<Long> ids) {
        Map<Long, Set<LocalDate>> map = new HashMap<>();
        for (HabitLog l : logs.findByHabitIdIn(ids)) {
            map.computeIfAbsent(l.getHabitId(), k -> new HashSet<>()).add(l.getLogDate());
        }
        return map;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
