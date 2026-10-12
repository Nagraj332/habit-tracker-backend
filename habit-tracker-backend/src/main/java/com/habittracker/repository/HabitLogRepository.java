package com.habittracker.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.habittracker.model.HabitLog;

public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {

    List<HabitLog> findByHabitIdIn(Collection<Long> habitIds);

    Optional<HabitLog> findByHabitIdAndLogDate(Long habitId, LocalDate logDate);

    @Query("select l.logDate from HabitLog l where l.habitId = :habitId")
    List<LocalDate> findDatesByHabitId(@Param("habitId") Long habitId);

    @Query("select l.logDate from HabitLog l where l.habitId = :habitId and l.logDate between :from and :to order by l.logDate")
    List<LocalDate> findDatesBetween(@Param("habitId") Long habitId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Modifying
    @Query("delete from HabitLog l where l.habitId = :habitId")
    void deleteAllByHabit(@Param("habitId") Long habitId);
}
