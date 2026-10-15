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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    private final boolean enabled; // set app.reminder.enabled=false to switch the server alarms off
    private Instant lastCleanup = Instant.EPOCH;

    // when the reminder job last ran (shown on the Reminders page, so a sleeping server is easy to spot)
    private static volatile Instant lastRun;

    public static Instant lastRun() {
        return lastRun;
    }

    // the most recent alarm sent for each owner and what the push service answered (shown on the Reminders page)
    private static final Map<String, String> lastAlarm = new ConcurrentHashMap<>();

    public static String lastAlarm(String owner) {
        return lastAlarm.get(owner);
    }

    public ReminderScheduler(PushSubscriptionRepository subscriptions, HabitRepository habits, HabitLogRepository logs,
                             ReminderStateRepository states, WebPushSender sender, ObjectMapper mapper,
                             @Value("${app.reminder.max-repeats:180}") int maxRepeats,
                             @Value("${app.reminder.window-minutes:180}") int windowMinutes,
                             @Value("${app.reminder.enabled:true}") boolean enabled) {
        this.subscriptions = subscriptions;
        this.habits = habits;
        this.logs = logs;
        this.states = states;
        this.sender = sender;
        this.mapper = mapper;
        this.maxRepeats = maxRepeats;
        this.window = Duration.ofMinutes(windowMinutes);
        this.enabled = enabled;
    }

    // first run 5 seconds after start-up, so a server that was just woken up sends overdue alarms straight away
    @Scheduled(fixedDelay = 60_000, initialDelay = 5_000)
    public void run() {
        if (!enabled) return;
        try {
            try {
                tick(Instant.now());
            } catch (Exception first) {
                // a database connection that went stale while the server slept: one quick retry on a fresh connection
                log.warn("Reminder run failed, retrying once: {}", first.toString());
                Thread.sleep(3000);
                tick(Instant.now());
            }
            cleanup(Instant.now());
        } catch (Exception e) {
            log.warn("Reminder run failed", e);
        } finally {
            lastRun = Instant.now();
        }
    }

    /** True while an alarm has rung, nobody has stopped it, and it is still inside its ringing window. */
    public boolean isRinging(ReminderState state, Instant now) {
        return !state.isStopped()
                && state.getSentCount() > 0
                && state.getSentCount() < maxRepeats
                && !now.isBefore(state.getStartAt())
                && !now.isAfter(state.getStartAt().plus(window));
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

        List<Integer> statuses = new ArrayList<>();
        for (PushSubscription device : new ArrayList<>(devices)) {
            int status = sender.send(device, payload, 120);
            statuses.add(status);
            if (WebPushSender.isGone(status)) {
                subscriptions.delete(device);
                devices.remove(device);
            }
        }
        String answers = statuses.stream().map(String::valueOf).collect(Collectors.joining(", "));
        log.info("Alarm #{} for habit {} sent, push service answered {}", state.getSentCount() + 1, habit.getId(), answers);
        lastAlarm.put(owner, now.atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm")) + " for " + habit.getName() + ", push service answered " + answers);

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
