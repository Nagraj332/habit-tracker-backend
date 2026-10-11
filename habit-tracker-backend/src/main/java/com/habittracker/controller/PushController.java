package com.habittracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habittracker.config.OwnerIds;
import com.habittracker.exception.ResourceNotFoundException;
import com.habittracker.model.Habit;
import com.habittracker.model.PushSubscription;
import com.habittracker.model.ReminderState;
import com.habittracker.push.ReminderScheduler;
import com.habittracker.push.VapidService;
import com.habittracker.push.WebPushSender;
import com.habittracker.repository.HabitLogRepository;
import com.habittracker.repository.HabitRepository;
import com.habittracker.repository.PushSubscriptionRepository;
import com.habittracker.repository.ReminderStateRepository;
import com.habittracker.service.HabitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Lets a browser sign up for alarm notifications and tell the server the alarm was handled. */
@RestController
@RequestMapping("/api/push")
public class PushController {

    private static final int MAX_DEVICES_PER_OWNER = 10;
    // only real browser push services may be called (stops the server being used to reach other addresses)
    private static final List<String> ALLOWED_HOSTS = List.of("googleapis.com", "push.services.mozilla.com", "push.apple.com", "notify.windows.com");

    public record Keys(String p256dh, String auth) {}
    public record SubscribeRequest(String endpoint, Keys keys, String timezone) {}
    public record UnsubscribeRequest(String endpoint) {}
    public record AckRequest(Long habitId, Integer snoozeMinutes, String tz) {}
    public record DoneRequest(Long habitId, String date, String tz) {}

    private final VapidService vapid;
    private final PushSubscriptionRepository subscriptions;
    private final ReminderStateRepository states;
    private final HabitRepository habits;
    private final HabitLogRepository logs;
    private final HabitService habitService;
    private final WebPushSender sender;
    private final ReminderScheduler scheduler;
    private final ObjectMapper mapper;
    private final Map<String, Instant> lastTest = new ConcurrentHashMap<>();

    public PushController(VapidService vapid, PushSubscriptionRepository subscriptions, ReminderStateRepository states,
                          HabitRepository habits, HabitLogRepository logs, HabitService habitService,
                          WebPushSender sender, ReminderScheduler scheduler, ObjectMapper mapper) {
        this.vapid = vapid;
        this.subscriptions = subscriptions;
        this.states = states;
        this.habits = habits;
        this.logs = logs;
        this.habitService = habitService;
        this.sender = sender;
        this.scheduler = scheduler;
        this.mapper = mapper;
    }

    @GetMapping("/key")
    public Map<String, String> key() {
        return Map.of("publicKey", vapid.publicKey());
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@RequestHeader(value = "X-Owner-Id", required = false) String owner,
                                          @RequestBody SubscribeRequest request) {
        String ownerId = OwnerIds.require(owner);
        if (request == null || request.keys() == null) throw new IllegalArgumentException("Invalid subscription");
        String endpoint = request.endpoint() == null ? "" : request.endpoint().trim();
        String p256dh = request.keys().p256dh();
        String auth = request.keys().auth();
        checkEndpoint(endpoint);
        if (p256dh == null || p256dh.length() > 200 || auth == null || auth.length() > 100) {
            throw new IllegalArgumentException("Invalid subscription keys");
        }

        String hash = sha256(endpoint);
        PushSubscription sub = subscriptions.findByEndpointHash(hash).orElse(null);
        if (sub == null) {
            if (subscriptions.countByOwnerId(ownerId) >= MAX_DEVICES_PER_OWNER) {
                throw new IllegalArgumentException("Too many devices for this account");
            }
            sub = new PushSubscription();
            sub.setEndpointHash(hash);
            sub.setEndpoint(endpoint);
        }
        sub.setOwnerId(ownerId);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        sub.setTimezone(validZone(request.timezone()));
        subscriptions.save(sub);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestHeader(value = "X-Owner-Id", required = false) String owner,
                                            @RequestBody UnsubscribeRequest request) {
        String ownerId = OwnerIds.require(owner);
        if (request != null && request.endpoint() != null) {
            subscriptions.findByEndpointHash(sha256(request.endpoint().trim()))
                    .filter(s -> s.getOwnerId().equals(ownerId))
                    .ifPresent(subscriptions::delete);
        }
        return ResponseEntity.noContent().build();
    }

    /** The user dismissed or snoozed the alarm: stop it (or ring again after the snooze). */
    @PostMapping("/ack")
    public ResponseEntity<Void> ack(@RequestHeader(value = "X-Owner-Id", required = false) String owner,
                                    @RequestBody AckRequest request) {
        String ownerId = OwnerIds.require(owner);
        Habit habit = ownedHabit(ownerId, request == null ? null : request.habitId());
        ZoneId zone = zoneFor(ownerId, request.tz());
        LocalDate today = LocalDate.now(zone);
        ReminderState state = stateFor(ownerId, habit, today);

        int snooze = request.snoozeMinutes() == null ? 0 : Math.max(0, Math.min(request.snoozeMinutes(), 120));
        if (snooze > 0) {
            state.setStartAt(Instant.now().plus(snooze, ChronoUnit.MINUTES));
            state.setSentCount(0);
            state.setLastSentAt(null);
            state.setStopped(false);
        } else {
            state.setStopped(true);
        }
        states.save(state);
        return ResponseEntity.noContent().build();
    }

    /** "Mark done" from the notification. Safe to call twice: it never un-marks a habit. */
    @PostMapping("/done")
    public ResponseEntity<Void> done(@RequestHeader(value = "X-Owner-Id", required = false) String owner,
                                     @RequestBody DoneRequest request) {
        String ownerId = OwnerIds.require(owner);
        Habit habit = ownedHabit(ownerId, request == null ? null : request.habitId());
        ZoneId zone = zoneFor(ownerId, request.tz());
        LocalDate day = (request.date() == null || request.date().isBlank()) ? LocalDate.now(zone) : LocalDate.parse(request.date());

        if (logs.findByHabitIdAndLogDate(habit.getId(), day).isEmpty()) {
            habitService.toggleCompletion(ownerId, habit.getId(), day);
        }
        ReminderState state = stateFor(ownerId, habit, LocalDate.now(zone));
        state.setStopped(true);
        states.save(state);
        return ResponseEntity.noContent().build();
    }

    /** Sends a test alarm notification to every device of this owner. */
    @PostMapping("/test")
    public Map<String, Object> test(@RequestHeader(value = "X-Owner-Id", required = false) String owner) throws Exception {
        String ownerId = OwnerIds.require(owner);
        Instant now = Instant.now();
        Instant last = lastTest.get(ownerId);
        if (last != null && last.plusSeconds(8).isAfter(now)) throw new IllegalArgumentException("Please wait a few seconds");
        lastTest.put(ownerId, now);

        byte[] payload = mapper.writeValueAsString(Map.of(
                "type", "test",
                "title", "Test alarm",
                "body", "Alarm alerts work on this device, even when the app is closed.",
                "tag", "test-alarm")).getBytes(StandardCharsets.UTF_8);

        int sent = 0;
        int devices = 0;
        for (PushSubscription s : subscriptions.findByOwnerId(ownerId)) {
            int status = sender.send(s, payload, 60);
            if (WebPushSender.isGone(status)) {
                subscriptions.delete(s);
                continue;
            }
            devices++;
            if (status >= 200 && status < 300) sent++;
        }
        // devices = registered phones/browsers that could be tried, sent = how many the push service accepted
        return Map.of("sent", sent, "devices", devices);
    }

    /** Small health report for the Reminders page: is this device registered, and is the server running its reminder job? */
    @GetMapping("/status")
    public Map<String, Object> status(@RequestHeader(value = "X-Owner-Id", required = false) String owner,
                                      @RequestParam(required = false) String tz) {
        String ownerId = OwnerIds.require(owner);
        ZoneId zone = zoneFor(ownerId, tz);
        Instant last = ReminderScheduler.lastRun();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("devices", subscriptions.countByOwnerId(ownerId));
        out.put("timezone", zone.getId());
        out.put("serverTime", ZonedDateTime.now(zone).format(DateTimeFormatter.ofPattern("HH:mm")));
        out.put("lastCheckSeconds", last == null ? null : Duration.between(last, Instant.now()).getSeconds());
        return out;
    }

    /** Habits whose alarm has rung and is still waiting for the user (used to pop the alarm up when the app is opened). */
    @GetMapping("/ringing")
    public List<Long> ringing(@RequestHeader(value = "X-Owner-Id", required = false) String owner,
                              @RequestParam(required = false) String tz) {
        String ownerId = OwnerIds.require(owner);
        ZoneId zone = zoneFor(ownerId, tz);
        LocalDate today = LocalDate.now(zone);
        Instant now = Instant.now();
        List<Long> ids = new ArrayList<>();
        for (ReminderState state : states.findByOwnerIdAndDay(ownerId, today)) {
            if (!scheduler.isRinging(state, now)) continue;
            if (logs.findByHabitIdAndLogDate(state.getHabitId(), today).isPresent()) continue;
            if (!habits.existsByIdAndOwnerId(state.getHabitId(), ownerId)) continue;
            ids.add(state.getHabitId());
        }
        return ids;
    }

    // ---------- helpers ----------

    private Habit ownedHabit(String ownerId, Long habitId) {
        if (habitId == null) throw new IllegalArgumentException("habitId is required");
        return habits.findByIdAndOwnerId(habitId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id " + habitId));
    }

    private ReminderState stateFor(String ownerId, Habit habit, LocalDate day) {
        return states.findByHabitIdAndDay(habit.getId(), day)
                .orElseGet(() -> new ReminderState(ownerId, habit.getId(), day, Instant.now()));
    }

    private ZoneId zoneFor(String ownerId, String tz) {
        if (tz != null && !tz.isBlank()) return ZoneId.of(validZone(tz));
        return subscriptions.findByOwnerId(ownerId).stream()
                .map(PushSubscription::getTimezone)
                .filter(z -> z != null && !z.isBlank())
                .findFirst()
                .map(ZoneId::of)
                .orElse(ZoneId.of("Asia/Kolkata"));
    }

    private static String validZone(String tz) {
        try {
            return tz == null || tz.isBlank() ? "Asia/Kolkata" : ZoneId.of(tz.trim()).getId();
        } catch (Exception e) {
            return "Asia/Kolkata";
        }
    }

    private static void checkEndpoint(String endpoint) {
        if (endpoint.isEmpty() || endpoint.length() > 1000) throw new IllegalArgumentException("Invalid endpoint");
        try {
            URI uri = URI.create(endpoint);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            boolean allowed = ALLOWED_HOSTS.stream().anyMatch(h -> host.equals(h) || host.endsWith("." + h));
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !allowed) throw new IllegalArgumentException("Unsupported push service");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid endpoint");
        }
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
