package com.habittracker.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "habits")
public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    // Hex color used by the frontend, e.g. "#4CAF50"
    @Column(length = 20)
    private String color;

    // Daily reminder as "HH:mm" (24-hour clock), or null when no reminder is set
    @Column(length = 5)
    private String reminderTime;

    // Specific days this habit is scheduled on. Empty means "every day from createdDate".
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "habit_dates", joinColumns = @JoinColumn(name = "habit_id"))
    @Column(name = "scheduled_date")
    private Set<LocalDate> scheduledDates = new HashSet<>();

    @Column(nullable = false)
    private LocalDate createdDate;

    @PrePersist
    void onCreate() {
        if (createdDate == null) createdDate = LocalDate.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Set<LocalDate> getScheduledDates() { return scheduledDates; }
    public void setScheduledDates(Set<LocalDate> scheduledDates) { this.scheduledDates = scheduledDates; }
    public String getReminderTime() { return reminderTime; }
    public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }
    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }
}
