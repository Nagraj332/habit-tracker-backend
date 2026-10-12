package com.habittracker.controller;

import com.habittracker.config.OwnerIds;
import com.habittracker.dto.HabitRequest;
import com.habittracker.dto.HabitResponse;
import com.habittracker.dto.HabitStats;
import com.habittracker.service.HabitService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/habits")
public class HabitController {

    private final HabitService habitService;

    public HabitController(HabitService habitService) {
        this.habitService = habitService;
    }

    @GetMapping
    public List<HabitResponse> getAll(@RequestHeader(value = "X-Owner-Id", required = false) String owner) {
        return habitService.getAllHabits(OwnerIds.require(owner));
    }

    @GetMapping("/{id}")
    public HabitResponse getOne(@RequestHeader(value = "X-Owner-Id", required = false) String owner,
                            @PathVariable Long id) {
        return habitService.getHabit(OwnerIds.require(owner), id);
    }

    @PostMapping
    public ResponseEntity<HabitResponse> create(@RequestHeader(value = "X-Owner-Id", required = false) String owner,
                                                @Valid @RequestBody HabitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(habitService.createHabit(OwnerIds.require(owner), request));
    }

    @PutMapping("/{id}")
    public HabitResponse update(@RequestHeader(value = "X-Owner-Id", required = false) String owner,
                               @PathVariable Long id, @Valid @RequestBody HabitRequest request) {
        return habitService.updateHabit(OwnerIds.require(owner), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader(value = "X-Owner-Id", required = false) String owner,
                                       @PathVariable Long id) {
        habitService.deleteHabit(OwnerIds.require(owner), id);
        return ResponseEntity.noContent().build();
    }

    /** Toggle completion. Optional ?date=yyyy-MM-dd (defaults to today). */
    @PostMapping("/{id}/toggle")
    public HabitResponse toggle(@RequestHeader(value = "X-Owner-Id", required = false) String owner,
                                @PathVariable Long id,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return habitService.toggleCompletion(OwnerIds.require(owner), id, date);
    }

    /** Completed dates for the heatmap. Defaults to the last 365 days. */
    @GetMapping("/{id}/logs")
    public List<LocalDate> logs(@RequestHeader(value = "X-Owner-Id", required = false) String owner,
                                @PathVariable Long id,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return habitService.getCompletedDates(OwnerIds.require(owner), id, from, to);
    }

    @GetMapping("/{id}/stats")
    public HabitStats stats(@RequestHeader(value = "X-Owner-Id", required = false) String owner,
                           @PathVariable Long id) {
        return habitService.getStats(OwnerIds.require(owner), id);
    }
}
