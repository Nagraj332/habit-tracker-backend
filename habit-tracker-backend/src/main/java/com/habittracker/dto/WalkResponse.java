package com.habittracker.dto;

import java.time.LocalDate;

public record WalkResponse(
        Long id,
        Long habitId,
        LocalDate walkDate,
        double distanceMeters,
        long durationSeconds,
        String activityType
) {}
