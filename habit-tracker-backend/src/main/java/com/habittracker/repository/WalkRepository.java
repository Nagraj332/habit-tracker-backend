package com.habittracker.repository;

import com.habittracker.model.Walk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WalkRepository extends JpaRepository<Walk, Long> {
    List<Walk> findByOwnerIdAndWalkDateBetweenOrderByWalkDateDescIdDesc(String ownerId, LocalDate from, LocalDate to);

    Optional<Walk> findByIdAndOwnerId(Long id, String ownerId);
}
