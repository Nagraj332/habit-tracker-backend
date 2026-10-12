package com.habittracker.model;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "habits", indexes = @Index(name = "idx_habits_owner", columnList = "owner_id"))
public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false, length = 64)
    private String ownerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(length = 20)
    private String color;

    /** "HH:mm", or null when there is no reminder. */
    @Column(name = "reminder_time", length = 5)
    private String reminderTime;

    /** For a daily habit: the day it starts. Otherwise the day it was created. */
    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate;

    /** Empty = a daily habit. Otherwise the habit is only due on these days. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "habit_scheduled_dates", joinColumns = @JoinColumn(name = "habit_id"))
    @Column(name = "scheduled_date", nullable = false)
    @OrderBy
    @Fetch(FetchMode.SUBSELECT)
    private Set<LocalDate> scheduledDates = new LinkedHashSet<>();

    public Long getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getReminderTime() { return reminderTime; }
    public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }
    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }
    public Set<LocalDate> getScheduledDates() { return scheduledDates; }
}
