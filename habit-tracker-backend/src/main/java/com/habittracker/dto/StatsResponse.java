package com.habittracker.dto;

public record StatsResponse(
        int currentStreak,
        int longestStreak,
        int totalCompletions,
        int completionRateLast30Days) {
}
