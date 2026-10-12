package com.habittracker.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/** One row = habit was completed on that date. */
@Entity
@Table(name = "habit_logs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"habit_id", "log_date"}))
public class HabitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "habit_id")
    private Habit habit;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    public HabitLog() {}

    public HabitLog(Habit habit, LocalDate logDate) {
        this.habit = habit;
        this.logDate = logDate;
    }

    public Long getId() { return id; }
    public Habit getHabit() { return habit; }
    public void setHabit(Habit habit) { this.habit = habit; }
    public LocalDate getLogDate() { return logDate; }
    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }
}
