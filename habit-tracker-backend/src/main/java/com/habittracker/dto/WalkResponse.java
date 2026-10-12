package com.habittracker.dto;

import java.time.LocalDate;

public record WalkResponse(
        Long id,
        Long habitId,
        LocalDate walkDate,
        int distanceMeters,
        int durationSeconds,
        String activityType) {
}
