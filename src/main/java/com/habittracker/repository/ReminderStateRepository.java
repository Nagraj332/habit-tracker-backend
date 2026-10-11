package com.habittracker.repository;

import com.habittracker.model.ReminderState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReminderStateRepository extends JpaRepository<ReminderState, Long> {
    Optional<ReminderState> findByHabitIdAndDay(Long habitId, LocalDate day);

    List<ReminderState> findByDayBefore(LocalDate day);
}
