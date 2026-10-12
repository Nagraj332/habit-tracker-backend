package com.habittracker.repository;

import com.habittracker.model.HabitLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {

    Optional<HabitLog> findByHabitIdAndLogDate(Long habitId, LocalDate logDate);

    List<HabitLog> findByHabitIdOrderByLogDateAsc(Long habitId);

    List<HabitLog> findByHabitIdAndLogDateBetweenOrderByLogDateAsc(Long habitId, LocalDate from, LocalDate to);

    // all check-ins of several habits in one query
    List<HabitLog> findByHabitIdIn(Collection<Long> habitIds);

    void deleteByHabitId(Long habitId);
}
