package com.habittracker.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habittracker.model.Habit;
import com.habittracker.model.PushSubscription;
import com.habittracker.model.ReminderState;
import com.habittracker.repository.HabitLogRepository;
import com.habittracker.repository.HabitRepository;
import com.habittracker.repository.PushSubscriptionRepository;
import com.habittracker.repository.ReminderStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs every minute. For each habit whose reminder time has come and that is not done yet,
 * it pushes an alarm notification to the owner's devices, and repeats every minute
 * until the user marks the habit done, stops the alarm, or the limit below is reached.
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
    static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");

    private final PushSubscriptionRepository subscriptions;
    private final HabitRepository habits;
    private final HabitLogRepository logs;
    private final ReminderStateRepository states;
    private final WebPushSender sender;
    private final ObjectMapper mapper;
    private final int maxRepeats;
    private final Duration window;
    private Instant lastCleanup = Instant.EPOCH;

    public ReminderScheduler(PushSubscriptionRepository subscriptions, HabitRepository habits, HabitLogRepository logs,
                             ReminderStateRepository states, WebPushSender sender, ObjectMapper mapper,
                             @Value("${app.reminder.max-repeats:30}") int maxRepeats,
                             @Value("${app.reminder.window-minutes:45}") int windowMinutes) {
        this.subscriptions = subscriptions;
        this.habits = habits;
        this.logs = logs;
        this.states = states;
        this.sender = sender;
        this.mapper = mapper;
        this.maxRepeats = maxRepeats;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void run() {
        try {
            tick(Instant.now());
            cleanup(Instant.now());
        } catch (Exception e) {
            log.warn("Reminder run failed", e);
        }
    }

    private void tick(Instant now) {
        List<PushSubscription> all = subscriptions.findAll();
        if (all.isEmpty()) return;

        Map<String, List<PushSubscription>> byOwner = new LinkedHashMap<>();
        for (PushSubscription s : all) {
            byOwner.computeIfAbsent(s.getOwnerId(), k -> new ArrayList<>()).add(s);
        }

        for (Map.Entry<String, List<PushSubscription>> entry : byOwner.entrySet()) {
            String owner = entry.getKey();
            List<PushSubscription> devices = new ArrayList<>(entry.getValue());
            devices.sort(Comparator.comparing(PushSubscription::getCreatedAt).reversed());
            ZoneId zone = zoneOf(devices.get(0).getTimezone());
            LocalDate today = now.atZone(zone).toLocalDate();

            for (Habit habit : habits.findByOwnerIdOrderByIdDesc(owner)) {
                if (devices.isEmpty()) break;
                try {
                    remind(owner, devices, habit, zone, today, now);
                } catch (Exception e) {
                    log.warn("Reminder for habit {} failed: {}", habit.getId(), e.toString());
                }
            }
        }
    }

    private void remind(String owner, List<PushSubscription> devices, Habit habit, ZoneId zone, LocalDate today, Instant now) throws Exception {
        if (habit.getReminderTime() == null || !isDueToday(habit, today)) return;
        if (logs.findByHabitIdAndLogDate(habit.getId(), today).isPresent()) return;

        Instant reminderAt = today.atTime(LocalTime.parse(habit.getReminderTime())).atZone(zone).toInstant();
        ReminderState state = states.findByHabitIdAndDay(habit.getId(), today).orElse(null);
        if (state == null) {
            if (now.isBefore(reminderAt)) return; // not time yet
            state = new ReminderState(owner, habit.getId(), today, reminderAt);
        }

        if (state.isStopped()) return;
        if (now.isBefore(state.getStartAt())) return; // snoozed
        if (now.isAfter(state.getStartAt().plus(window))) return; // too late, stop pestering
        if (state.getSentCount() >= maxRepeats) return;
        if (state.getLastSentAt() != null && Duration.between(state.getLastSentAt(), now).getSeconds() < 50) return;

        String description = habit.getDescription();
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "habit");
        message.put("title", "Time for " + habit.getName());
        message.put("body", description == null || description.isBlank() ? "You haven't done this one yet today." : description);
        message.put("habitId", habit.getId());
        message.put("tag", "habit-" + habit.getId());
        message.put("count", state.getSentCount() + 1);
        byte[] payload = mapper.writeValueAsString(message).getBytes(StandardCharsets.UTF_8);

        for (PushSubscription device : new ArrayList<>(devices)) {
            int status = sender.send(device, payload, 120);
            if (WebPushSender.isGone(status)) {
                subscriptions.delete(device);
                devices.remove(device);
            }
        }

        state.setSentCount(state.getSentCount() + 1);
        state.setLastSentAt(now);
        states.save(state);
    }

    /** Same rule as the frontend: chosen dates only, or every day from the start date. */
    static boolean isDueToday(Habit habit, LocalDate today) {
        if (!habit.getScheduledDates().isEmpty()) return habit.getScheduledDates().contains(today);
        return !habit.getCreatedDate().isAfter(today);
    }

    static ZoneId zoneOf(String name) {
        try {
            return name == null || name.isBlank() ? DEFAULT_ZONE : ZoneId.of(name);
        } catch (Exception e) {
            return DEFAULT_ZONE;
        }
    }

    private void cleanup(Instant now) {
        if (Duration.between(lastCleanup, now).toHours() < 6) return;
        lastCleanup = now;
        states.deleteAll(states.findByDayBefore(LocalDate.now().minusDays(3)));
    }
}
