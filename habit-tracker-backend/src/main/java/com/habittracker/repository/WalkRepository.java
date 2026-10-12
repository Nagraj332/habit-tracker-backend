package com.habittracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.habittracker.model.Walk;

public interface WalkRepository extends JpaRepository<Walk, Long> {

    List<Walk> findByOwnerIdOrderByWalkDateDescIdDesc(String ownerId);

    Optional<Walk> findByIdAndOwnerId(Long id, String ownerId);

    /** When a habit is deleted its walks stay, but no longer point at it. */
    @Modifying
    @Query("update Walk w set w.habitId = null where w.habitId = :habitId")
    int detachHabit(@Param("habitId") Long habitId);
}
