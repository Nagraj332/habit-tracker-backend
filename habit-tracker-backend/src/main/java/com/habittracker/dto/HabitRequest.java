package com.habittracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record HabitRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Size(max = 255, message = "Description must be at most 255 characters")
        String description,

        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Color must be a hex value like #4CAF50")
        String color,

        @NotBlank(message = "Reminder time is required")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Reminder must be a time like 07:30")
        String reminderTime,

        // Day the habit starts (may be in the future). Null means today.
        LocalDate startDate,

        // Specific days for this habit. Null leaves them unchanged, empty means every day.
        @Size(max = 366, message = "Choose at most 366 dates")
        List<LocalDate> scheduledDates
) {}
