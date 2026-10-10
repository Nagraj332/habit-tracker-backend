package com.habittracker.dto;

public record HabitStats(
        Long habitId,
        int currentStreak,
        int longestStreak,
        long totalCompletions,
        double completionRateLast30Days   // percentage, 0-100
) {}
