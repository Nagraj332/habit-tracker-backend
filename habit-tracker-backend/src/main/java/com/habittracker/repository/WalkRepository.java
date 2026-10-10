package com.habittracker.repository;

import com.habittracker.model.Walk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface WalkRepository extends JpaRepository<Walk, Long> {
    List<Walk> findByWalkDateBetweenOrderByWalkDateDescIdDesc(LocalDate from, LocalDate to);
}
