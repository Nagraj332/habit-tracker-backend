package com.habittracker.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body of POST /habits and PUT /habits/{id}. */
public record HabitRequest(
        @NotBlank(message = "Give your habit a name.")
        @Size(max = 100, message = "The name can have at most 100 characters.")
        String name,

        @Size(max = 255, message = "The description can have at most 255 characters.")
        String description,

        @Size(max = 20, message = "Invalid colour.")
        String color,

        @Pattern(regexp = "^$|^([01]\\d|2[0-3]):[0-5]\\d$", message = "The reminder time must look like 08:30.")
        String reminderTime,

        /** Start day of a daily habit. */
        LocalDate startDate,

        /** Days of a habit that is only done on chosen dates. Empty = daily. */
        @Size(max = 1000, message = "Too many dates selected.")
        List<LocalDate> scheduledDates) {
}
