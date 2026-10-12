package com.habittracker.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.habittracker.dto.WalkRequest;
import com.habittracker.dto.WalkResponse;
import com.habittracker.exception.BadRequestException;
import com.habittracker.exception.NotFoundException;
import com.habittracker.model.Walk;
import com.habittracker.repository.HabitRepository;
import com.habittracker.repository.WalkRepository;

@Service
public class WalkService {

    private final WalkRepository walks;
    private final HabitRepository habits;

    public WalkService(WalkRepository walks, HabitRepository habits) {
        this.walks = walks;
        this.habits = habits;
    }

    @Transactional(readOnly = true)
    public List<WalkResponse> list(String owner) {
        return walks.findByOwnerIdOrderByWalkDateDescIdDesc(owner).stream().map(WalkService::toResponse).toList();
    }

    @Transactional
    public WalkResponse create(String owner, WalkRequest req) {
        if (req.habitId() != null && habits.findByIdAndOwnerId(req.habitId(), owner).isEmpty()) {
            throw new BadRequestException("That habit doesn't exist.");
        }
        Walk w = new Walk();
        w.setOwnerId(owner);
        w.setHabitId(req.habitId());
        w.setWalkDate(req.walkDate());
        w.setDistanceMeters(req.distanceMeters());
        w.setDurationSeconds(req.durationSeconds());
        w.setActivityType(req.activityType() == null || req.activityType().isBlank() ? "WALK" : req.activityType());
        return toResponse(walks.save(w));
    }

    @Transactional
    public void delete(String owner, Long id) {
        Walk w = walks.findByIdAndOwnerId(id, owner).orElseThrow(() -> new NotFoundException("Walk not found."));
        walks.delete(w);
    }

    private static WalkResponse toResponse(Walk w) {
        return new WalkResponse(w.getId(), w.getHabitId(), w.getWalkDate(), w.getDistanceMeters(),
                w.getDurationSeconds(), w.getActivityType());
    }
}
