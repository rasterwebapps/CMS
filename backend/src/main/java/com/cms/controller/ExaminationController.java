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

import com.cms.dto.ExaminationRequest;
import com.cms.dto.ExaminationResponse;
import com.cms.service.ExaminationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/examinations")
public class ExaminationController {

    private final ExaminationService examinationService;

    public ExaminationController(ExaminationService examinationService) {
        this.examinationService = examinationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FACULTY')")
    public ResponseEntity<ExaminationResponse> create(@Valid @RequestBody ExaminationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examinationService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ExaminationResponse>> findAll() {
        return ResponseEntity.ok(examinationService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExaminationResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(examinationService.findById(id));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<ExaminationResponse>> findByCourseId(@PathVariable Long courseId) {
        return ResponseEntity.ok(examinationService.findByCourseId(courseId));
    }

    @GetMapping("/semester/{semesterId}")
    public ResponseEntity<List<ExaminationResponse>> findBySemesterId(@PathVariable Long semesterId) {
        return ResponseEntity.ok(examinationService.findBySemesterId(semesterId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FACULTY')")
    public ResponseEntity<ExaminationResponse> update(@PathVariable Long id,
                                                       @Valid @RequestBody ExaminationRequest request) {
        return ResponseEntity.ok(examinationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        examinationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
