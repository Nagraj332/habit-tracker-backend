package com.habittracker.dto;

import java.time.LocalDate;
import java.util.List;

public record HabitResponse(
        Long id,
        String name,
        String description,
        String color,
        LocalDate createdDate,
        String reminderTime,
        List<LocalDate> scheduledDates,
        boolean completedToday,
        int currentStreak
) {}
