package com.habittracker.controller;

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
    public List<HabitResponse> getAll() {
        return habitService.getAllHabits();
    }

    @GetMapping("/{id}")
    public HabitResponse getOne(@PathVariable Long id) {
        return habitService.getHabit(id);
    }

    @PostMapping
    public ResponseEntity<HabitResponse> create(@Valid @RequestBody HabitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(habitService.createHabit(request));
    }

    @PutMapping("/{id}")
    public HabitResponse update(@PathVariable Long id, @Valid @RequestBody HabitRequest request) {
        return habitService.updateHabit(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        habitService.deleteHabit(id);
        return ResponseEntity.noContent().build();
    }

    /** Toggle completion. Optional ?date=yyyy-MM-dd (defaults to today). */
    @PostMapping("/{id}/toggle")
    public HabitResponse toggle(@PathVariable Long id,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return habitService.toggleCompletion(id, date);
    }

    /** Completed dates for the heatmap. Defaults to the last 365 days. */
    @GetMapping("/{id}/logs")
    public List<LocalDate> logs(@PathVariable Long id,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return habitService.getCompletedDates(id, from, to);
    }

    @GetMapping("/{id}/stats")
    public HabitStats stats(@PathVariable Long id) {
        return habitService.getStats(id);
    }
}
