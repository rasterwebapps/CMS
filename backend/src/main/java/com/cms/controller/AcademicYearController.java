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

import com.cms.dto.AcademicYearRequest;
import com.cms.dto.AcademicYearResponse;
import com.cms.service.AcademicYearService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/academic-years")
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    public AcademicYearController(AcademicYearService academicYearService) {
        this.academicYearService = academicYearService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<AcademicYearResponse> create(@Valid @RequestBody AcademicYearRequest request) {
        AcademicYearResponse response = academicYearService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AcademicYearResponse>> findAll() {
        List<AcademicYearResponse> academicYears = academicYearService.findAll();
        return ResponseEntity.ok(academicYears);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcademicYearResponse> findById(@PathVariable Long id) {
        AcademicYearResponse response = academicYearService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/current")
    public ResponseEntity<AcademicYearResponse> findCurrent() {
        AcademicYearResponse response = academicYearService.findCurrent();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<AcademicYearResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AcademicYearRequest request) {
        AcademicYearResponse response = academicYearService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        academicYearService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
