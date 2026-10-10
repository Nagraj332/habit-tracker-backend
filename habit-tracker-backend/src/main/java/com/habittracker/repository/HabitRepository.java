package com.habittracker.repository;

import com.habittracker.model.Habit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HabitRepository extends JpaRepository<Habit, Long> {
    List<Habit> findAllByOrderByIdDesc();
}
