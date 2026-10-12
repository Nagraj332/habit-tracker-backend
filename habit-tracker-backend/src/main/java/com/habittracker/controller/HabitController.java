package com.habittracker.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.habittracker.config.Owner;
import com.habittracker.dto.HabitRequest;
import com.habittracker.dto.HabitResponse;
import com.habittracker.dto.StatsResponse;
import com.habittracker.service.HabitService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/habits")
public class HabitController {

    private final HabitService service;

    public HabitController(HabitService service) {
        this.service = service;
    }

    @GetMapping
    public List<HabitResponse> list(@RequestHeader(value = Owner.HEADER, required = false) String owner) {
        return service.list(Owner.require(owner));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HabitResponse create(@RequestHeader(value = Owner.HEADER, required = false) String owner,
                                @Valid @RequestBody HabitRequest body) {
        return service.create(Owner.require(owner), body);
    }

    @PutMapping("/{id}")
    public HabitResponse update(@RequestHeader(value = Owner.HEADER, required = false) String owner,
                                @PathVariable("id") Long id,
                                @Valid @RequestBody HabitRequest body) {
        return service.update(Owner.require(owner), id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(value = Owner.HEADER, required = false) String owner,
                       @PathVariable("id") Long id) {
        service.delete(Owner.require(owner), id);
    }

    /** Check the habit off for a day (default: today), or uncheck it if it was already done. */
    @PostMapping("/{id}/toggle")
    public HabitResponse toggle(@RequestHeader(value = Owner.HEADER, required = false) String owner,
                                @PathVariable("id") Long id,
                                @RequestParam(value = "date", required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.toggle(Owner.require(owner), id, date);
    }

    /** The days in [from, to] on which the habit was done, as yyyy-MM-dd strings. */
    @GetMapping("/{id}/logs")
    public List<LocalDate> logs(@RequestHeader(value = Owner.HEADER, required = false) String owner,
                                @PathVariable("id") Long id,
                                @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.logs(Owner.require(owner), id, from, to);
    }

    @GetMapping("/{id}/stats")
    public StatsResponse stats(@RequestHeader(value = Owner.HEADER, required = false) String owner,
                               @PathVariable("id") Long id) {
        return service.stats(Owner.require(owner), id);
    }
}
