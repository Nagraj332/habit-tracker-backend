package com.habittracker.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record WalkRequest(
        Long habitId,

        @NotNull(message = "The walk needs a date.")
        LocalDate walkDate,

        @NotNull(message = "Enter a distance.")
        @Positive(message = "The distance must be more than 0.")
        @Max(value = 500_000, message = "That distance is too long.")
        Integer distanceMeters,

        @NotNull(message = "Enter the time you walked.")
        @Positive(message = "The time must be more than 0.")
        @Max(value = 86_400, message = "That time is too long.")
        Integer durationSeconds,

        @Pattern(regexp = "^(WALK|RUN)?$", message = "The activity must be WALK or RUN.")
        String activityType) {
}
