package com.habittracker.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.habittracker.config.Owner;
import com.habittracker.dto.WalkRequest;
import com.habittracker.dto.WalkResponse;
import com.habittracker.service.WalkService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/walks")
public class WalkController {

    private final WalkService service;

    public WalkController(WalkService service) {
        this.service = service;
    }

    @GetMapping
    public List<WalkResponse> list(@RequestHeader(value = Owner.HEADER, required = false) String owner) {
        return service.list(Owner.require(owner));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalkResponse create(@RequestHeader(value = Owner.HEADER, required = false) String owner,
                               @Valid @RequestBody WalkRequest body) {
        return service.create(Owner.require(owner), body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(value = Owner.HEADER, required = false) String owner,
                       @PathVariable("id") Long id) {
        service.delete(Owner.require(owner), id);
    }
}
