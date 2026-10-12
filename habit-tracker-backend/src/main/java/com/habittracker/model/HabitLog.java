package com.habittracker.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** One row = the habit was done on that day. */
@Entity
@Table(name = "habit_logs",
        uniqueConstraints = @UniqueConstraint(name = "uk_habit_log_day", columnNames = {"habit_id", "log_date"}))
public class HabitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "habit_id", nullable = false)
    private Long habitId;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    protected HabitLog() {}

    public HabitLog(Long habitId, LocalDate logDate) {
        this.habitId = habitId;
        this.logDate = logDate;
    }

    public Long getId() { return id; }
    public Long getHabitId() { return habitId; }
    public LocalDate getLogDate() { return logDate; }
}
