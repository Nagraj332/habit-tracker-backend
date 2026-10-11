package com.habittracker.repository;

import com.habittracker.model.VapidKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VapidKeyRepository extends JpaRepository<VapidKey, Long> {
}
