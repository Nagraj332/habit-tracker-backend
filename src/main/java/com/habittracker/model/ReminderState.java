package com.habittracker.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/** Tracks the alarm of one habit on one day: how often it rang, and whether the user stopped it. */
@Entity
@Table(name = "reminder_states",
       uniqueConstraints = @UniqueConstraint(columnNames = {"habit_id", "reminder_day"}))
public class ReminderState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false, length = 64)
    private String ownerId;

    @Column(name = "habit_id", nullable = false)
    private Long habitId;

    @Column(name = "reminder_day", nullable = false)
    private LocalDate day;

    // the alarm starts ringing at this moment (the reminder time, or later after a snooze)
    @Column(nullable = false)
    private Instant startAt;

    private int sentCount;

    private Instant lastSentAt;

    // true once the user marked done, dismissed or stopped the alarm
    private boolean stopped;

    public ReminderState() {}

    public ReminderState(String ownerId, Long habitId, LocalDate day, Instant startAt) {
        this.ownerId = ownerId;
        this.habitId = habitId;
        this.day = day;
        this.startAt = startAt;
    }

    public Long getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public Long getHabitId() { return habitId; }
    public LocalDate getDay() { return day; }
    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }
    public int getSentCount() { return sentCount; }
    public void setSentCount(int sentCount) { this.sentCount = sentCount; }
    public Instant getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(Instant lastSentAt) { this.lastSentAt = lastSentAt; }
    public boolean isStopped() { return stopped; }
    public void setStopped(boolean stopped) { this.stopped = stopped; }
}
