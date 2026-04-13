package com.cms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.SyllabusRequest;
import com.cms.dto.SyllabusResponse;
import com.cms.service.SyllabusService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/syllabi")
public class SyllabusController {

    private final SyllabusService syllabusService;

    public SyllabusController(SyllabusService syllabusService) {
        this.syllabusService = syllabusService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FACULTY')")
    public ResponseEntity<SyllabusResponse> create(@Valid @RequestBody SyllabusRequest request) {
        SyllabusResponse response = syllabusService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SyllabusResponse>> findAll(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Boolean activeOnly) {
        List<SyllabusResponse> syllabusList;
        if (courseId != null && Boolean.TRUE.equals(activeOnly)) {
            syllabusList = List.of(syllabusService.findActiveByCourseId(courseId));
        } else if (courseId != null) {
            syllabusList = syllabusService.findByCourseId(courseId);
        } else {
            syllabusList = syllabusService.findAll();
        }
        return ResponseEntity.ok(syllabusList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SyllabusResponse> findById(@PathVariable Long id) {
        SyllabusResponse response = syllabusService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FACULTY')")
    public ResponseEntity<SyllabusResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SyllabusRequest request) {
        SyllabusResponse response = syllabusService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        syllabusService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
