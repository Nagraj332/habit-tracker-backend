package com.habittracker.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/** One recorded walk. habitId is optional (the habit it counted towards). */
@Entity
@Table(name = "walks")
public class Walk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long habitId;

    @Column(nullable = false)
    private LocalDate walkDate;

    @Column(nullable = false)
    private double distanceMeters;

    @Column(nullable = false)
    private long durationSeconds;

    // "WALK" or "RUN" (null on older rows means WALK)
    @Column(length = 10)
    private String activityType;

    public Long getId() { return id; }
    public Long getHabitId() { return habitId; }
    public void setHabitId(Long habitId) { this.habitId = habitId; }
    public LocalDate getWalkDate() { return walkDate; }
    public void setWalkDate(LocalDate walkDate) { this.walkDate = walkDate; }
    public double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(double distanceMeters) { this.distanceMeters = distanceMeters; }
    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }
    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }
}
