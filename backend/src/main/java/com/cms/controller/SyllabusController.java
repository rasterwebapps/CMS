package com.cms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.SyllabusActivationRequest;
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
    @PreAuthorize("@perm.has('SYLLABUS_MANAGE')")
    public ResponseEntity<SyllabusResponse> create(@Valid @RequestBody SyllabusRequest request) {
        SyllabusResponse response = syllabusService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SyllabusResponse>> findAll(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Boolean activeOnly) {
        List<SyllabusResponse> syllabusList;
        if (subjectId != null && Boolean.TRUE.equals(activeOnly)) {
            syllabusList = List.of(syllabusService.findActiveBySubjectId(subjectId));
        } else if (subjectId != null) {
            syllabusList = syllabusService.findBySubjectId(subjectId);
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

    /** A syllabus version is immutable once created — this only ever toggles isActive.
     *  Content changes must go through create() as a new version instead. There is no
     *  delete endpoint: versions are permanent, append-only history. */
    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('SYLLABUS_MANAGE')")
    public ResponseEntity<SyllabusResponse> setActive(
            @PathVariable Long id,
            @Valid @RequestBody SyllabusActivationRequest request) {
        SyllabusResponse response = syllabusService.setActive(id, request);
        return ResponseEntity.ok(response);
    }
}
