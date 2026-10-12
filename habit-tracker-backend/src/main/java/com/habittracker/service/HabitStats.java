package com.habittracker.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

import com.habittracker.dto.StatsResponse;

/**
 * Streak and completion maths. Pure functions (no database), so they are easy to test.
 *
 * A habit is "due" on a day when:
 *   - it has scheduled dates and the day is one of them, or
 *   - it is a daily habit and the day is on/after its start (created) date.
 * Days the habit is not due are skipped: they neither extend nor break a streak.
 * Today does not break a streak while it is still unchecked.
 */
public final class HabitStats {

    private static final int MAX_LOOKBACK_DAYS = 3650;

    private HabitStats() {}

    public static boolean isDue(LocalDate createdDate, Set<LocalDate> scheduled, LocalDate day) {
        if (!scheduled.isEmpty()) return scheduled.contains(day);
        return !day.isBefore(createdDate);
    }

    private static LocalDate firstDay(LocalDate createdDate, Set<LocalDate> scheduled, LocalDate today) {
        LocalDate first = scheduled.isEmpty() ? createdDate : Collections.min(scheduled);
        LocalDate floor = today.minusDays(MAX_LOOKBACK_DAYS);
        return first.isBefore(floor) ? floor : first;
    }

    public static int currentStreak(LocalDate createdDate, Set<LocalDate> scheduled, Set<LocalDate> done, LocalDate today) {
        LocalDate start = firstDay(createdDate, scheduled, today);
        LocalDate day = today;
        if (isDue(createdDate, scheduled, today) && !done.contains(today)) {
            day = today.minusDays(1); // today is still open, so it can't break the streak
        }
        int streak = 0;
        while (!day.isBefore(start)) {
            if (isDue(createdDate, scheduled, day)) {
                if (done.contains(day)) streak++;
                else break;
            }
            day = day.minusDays(1);
        }
        return streak;
    }

    public static int longestStreak(LocalDate createdDate, Set<LocalDate> scheduled, Set<LocalDate> done, LocalDate today) {
        int best = 0;
        int run = 0;
        for (LocalDate day = firstDay(createdDate, scheduled, today); !day.isAfter(today); day = day.plusDays(1)) {
            if (!isDue(createdDate, scheduled, day)) continue;
            if (done.contains(day)) {
                run++;
                best = Math.max(best, run);
            } else if (!day.equals(today)) {
                run = 0;
            }
        }
        return best;
    }

    /** Percentage (0-100) of due days in the last 30 days that were done. An unchecked today is not counted yet. */
    public static int completionRateLast30Days(LocalDate createdDate, Set<LocalDate> scheduled, Set<LocalDate> done, LocalDate today) {
        int total = 0;
        int ok = 0;
        for (int i = 29; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            if (!isDue(createdDate, scheduled, day)) continue;
            boolean isDone = done.contains(day);
            if (day.equals(today) && !isDone) continue;
            total++;
            if (isDone) ok++;
        }
        return total == 0 ? 0 : (int) Math.round(ok * 100.0 / total);
    }

    public static StatsResponse compute(LocalDate createdDate, Set<LocalDate> scheduled, Set<LocalDate> done, LocalDate today) {
        return new StatsResponse(
                currentStreak(createdDate, scheduled, done, today),
                longestStreak(createdDate, scheduled, done, today),
                done.size(),
                completionRateLast30Days(createdDate, scheduled, done, today));
    }
}
