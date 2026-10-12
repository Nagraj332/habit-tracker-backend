package com.habittracker.controller;

import com.habittracker.config.OwnerIds;
import com.habittracker.dto.WalkRequest;
import com.habittracker.dto.WalkResponse;
import com.habittracker.exception.ResourceNotFoundException;
import com.habittracker.model.Walk;
import com.habittracker.repository.HabitRepository;
import com.habittracker.repository.WalkRepository;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/walks")
public class WalkController {

    private static final double MAX_DISTANCE_METERS = 200_000;

    private final WalkRepository walks;
    private final HabitRepository habits;

    public WalkController(WalkRepository walks, HabitRepository habits) {
        this.walks = walks;
        this.habits = habits;
    }

    /** Recent walks of this owner, newest first. Defaults to the last 90 days. */
    @GetMapping
    public List<WalkResponse> list(@RequestHeader(value = "X-Owner-Id", required = false) String owner,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String ownerId = OwnerIds.require(owner);
        LocalDate end = (to != null) ? to : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(89);
        return walks.findByOwnerIdAndWalkDateBetweenOrderByWalkDateDescIdDesc(ownerId, start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<WalkResponse> create(@RequestHeader(value = "X-Owner-Id", required = false) String owner,
                                               @Valid @RequestBody WalkRequest request) {
        String ownerId = OwnerIds.require(owner);
        if (request.distanceMeters() > MAX_DISTANCE_METERS) {
            throw new IllegalArgumentException("Distance is too large");
        }
        if (request.habitId() != null && !habits.existsByIdAndOwnerId(request.habitId(), ownerId)) {
            throw new ResourceNotFoundException("Habit not found with id " + request.habitId());
        }
        Walk walk = new Walk();
        walk.setOwnerId(ownerId);
        walk.setHabitId(request.habitId());
        walk.setWalkDate(request.walkDate());
        walk.setDistanceMeters(request.distanceMeters());
        walk.setDurationSeconds(request.durationSeconds());
        walk.setActivityType(request.activityType() == null ? "WALK" : request.activityType());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(walks.save(walk)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader(value = "X-Owner-Id", required = false) String owner,
                                       @PathVariable Long id) {
        String ownerId = OwnerIds.require(owner);
        Walk walk = walks.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Walk not found with id " + id));
        walks.delete(walk);
        return ResponseEntity.noContent().build();
    }

    private WalkResponse toResponse(Walk w) {
        return new WalkResponse(w.getId(), w.getHabitId(), w.getWalkDate(), w.getDistanceMeters(), w.getDurationSeconds(),
                w.getActivityType() == null ? "WALK" : w.getActivityType());
    }
}
