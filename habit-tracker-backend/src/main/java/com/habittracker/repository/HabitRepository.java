package com.habittracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.habittracker.model.Habit;

public interface HabitRepository extends JpaRepository<Habit, Long> {

    List<Habit> findByOwnerIdOrderByIdAsc(String ownerId);

    Optional<Habit> findByIdAndOwnerId(Long id, String ownerId);
}
