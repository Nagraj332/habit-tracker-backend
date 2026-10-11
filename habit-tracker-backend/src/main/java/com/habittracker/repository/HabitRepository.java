package com.habittracker.repository;

import com.habittracker.model.Habit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HabitRepository extends JpaRepository<Habit, Long> {
    // loads the scheduled dates in the same query (no extra query per habit)
    @EntityGraph(attributePaths = "scheduledDates")
    List<Habit> findByOwnerIdOrderByIdDesc(String ownerId);

    Optional<Habit> findByIdAndOwnerId(Long id, String ownerId);

    boolean existsByIdAndOwnerId(Long id, String ownerId);
}
