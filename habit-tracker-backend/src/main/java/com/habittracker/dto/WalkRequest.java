package com.habittracker.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record WalkRequest(
        Long habitId,

        @NotNull(message = "Date is required")
        LocalDate walkDate,

        @NotNull(message = "Distance is required")
        @Positive(message = "Distance must be greater than zero")
        Double distanceMeters,

        @NotNull(message = "Duration is required")
        @Min(value = 1, message = "Duration must be at least 1 second")
        Long durationSeconds,

        @Pattern(regexp = "^(WALK|RUN)$", message = "Activity must be WALK or RUN")
        String activityType
) {}
