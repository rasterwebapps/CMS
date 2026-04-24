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
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.SemesterRequest;
import com.cms.dto.SemesterResponse;
import com.cms.service.SemesterService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/semesters")
public class SemesterController {

    private final SemesterService semesterService;

    public SemesterController(SemesterService semesterService) {
        this.semesterService = semesterService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<SemesterResponse> create(@Valid @RequestBody SemesterRequest request) {
        SemesterResponse response = semesterService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SemesterResponse>> findAll() {
        List<SemesterResponse> semesters = semesterService.findAll();
        return ResponseEntity.ok(semesters);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SemesterResponse> findById(@PathVariable Long id) {
        SemesterResponse response = semesterService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/academic-year/{academicYearId}")
    public ResponseEntity<List<SemesterResponse>> findByAcademicYearId(@PathVariable Long academicYearId) {
        List<SemesterResponse> semesters = semesterService.findByAcademicYearId(academicYearId);
        return ResponseEntity.ok(semesters);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<SemesterResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SemesterRequest request) {
        SemesterResponse response = semesterService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        semesterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
