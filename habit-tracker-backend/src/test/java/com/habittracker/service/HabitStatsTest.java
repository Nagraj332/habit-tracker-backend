package com.habittracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.habittracker.dto.StatsResponse;

class HabitStatsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    private static LocalDate d(int month, int day) {
        return LocalDate.of(2026, month, day);
    }

    @Test
    void dailyHabit_todayStillOpen_doesNotBreakStreak() {
        Set<LocalDate> done = Set.of(d(10, 8), d(10, 9), d(10, 10), d(10, 11));
        StatsResponse s = HabitStats.compute(d(10, 1), Set.of(), done, TODAY);
        assertEquals(4, s.currentStreak());
        assertEquals(4, s.longestStreak());
        assertEquals(4, s.totalCompletions());
        assertEquals(36, s.completionRateLast30Days()); // 4 of 11 finished days
    }

    @Test
    void dailyHabit_doneToday_extendsStreak() {
        Set<LocalDate> done = Set.of(d(10, 8), d(10, 9), d(10, 10), d(10, 11), d(10, 12));
        StatsResponse s = HabitStats.compute(d(10, 1), Set.of(), done, TODAY);
        assertEquals(5, s.currentStreak());
        assertEquals(42, s.completionRateLast30Days()); // 5 of 12
    }

    @Test
    void dailyHabit_missedYesterday_resetsCurrentStreak() {
        Set<LocalDate> done = Set.of(d(10, 9), d(10, 10));
        StatsResponse s = HabitStats.compute(d(10, 1), Set.of(), done, TODAY);
        assertEquals(0, s.currentStreak());
        assertEquals(2, s.longestStreak());
    }

    @Test
    void scheduledHabit_skipsDaysThatAreNotDue() {
        Set<LocalDate> scheduled = Set.of(d(10, 5), d(10, 8), d(10, 12));
        Set<LocalDate> done = Set.of(d(10, 5), d(10, 8));
        StatsResponse s = HabitStats.compute(d(10, 1), scheduled, done, TODAY);
        assertEquals(2, s.currentStreak());
        assertEquals(2, s.longestStreak());
        assertEquals(100, s.completionRateLast30Days());
    }

    @Test
    void habitThatStartsInTheFuture_isAllZeros() {
        StatsResponse s = HabitStats.compute(d(10, 20), Set.of(), Set.of(), TODAY);
        assertEquals(0, s.currentStreak());
        assertEquals(0, s.longestStreak());
        assertEquals(0, s.totalCompletions());
        assertEquals(0, s.completionRateLast30Days());
    }

    @Test
    void longestStreak_isKeptAfterAGap() {
        Set<LocalDate> done = Set.of(d(9, 2), d(9, 3), d(9, 4), d(9, 5), d(9, 6), d(9, 10), d(9, 11));
        StatsResponse s = HabitStats.compute(d(9, 1), Set.of(), done, TODAY);
        assertEquals(5, s.longestStreak());
        assertEquals(0, s.currentStreak());
    }
}
