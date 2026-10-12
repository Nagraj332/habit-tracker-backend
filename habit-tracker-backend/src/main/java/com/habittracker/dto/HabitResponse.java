package com.habittracker.dto;

import java.time.LocalDate;
import java.util.List;

public record HabitResponse(
        Long id,
        String name,
        String description,
        String color,
        String reminderTime,
        LocalDate startDate,
        LocalDate createdDate,
        List<LocalDate> scheduledDates,
        boolean completedToday,
        int currentStreak) {
}
