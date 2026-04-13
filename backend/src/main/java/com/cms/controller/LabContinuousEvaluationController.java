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

import com.cms.dto.LabContinuousEvaluationRequest;
import com.cms.dto.LabContinuousEvaluationResponse;
import com.cms.service.LabContinuousEvaluationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/lab-evaluations")
public class LabContinuousEvaluationController {

    private final LabContinuousEvaluationService labContinuousEvaluationService;

    public LabContinuousEvaluationController(LabContinuousEvaluationService labContinuousEvaluationService) {
        this.labContinuousEvaluationService = labContinuousEvaluationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_FACULTY') or hasRole('ROLE_LAB_INCHARGE')")
    public ResponseEntity<LabContinuousEvaluationResponse> create(
            @Valid @RequestBody LabContinuousEvaluationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(labContinuousEvaluationService.create(request));
    }

    @GetMapping("/experiment/{experimentId}")
    public ResponseEntity<List<LabContinuousEvaluationResponse>> findByExperimentId(@PathVariable Long experimentId) {
        return ResponseEntity.ok(labContinuousEvaluationService.findByExperimentId(experimentId));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<LabContinuousEvaluationResponse>> findByStudentId(@PathVariable Long studentId) {
        return ResponseEntity.ok(labContinuousEvaluationService.findByStudentId(studentId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LabContinuousEvaluationResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(labContinuousEvaluationService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_FACULTY') or hasRole('ROLE_LAB_INCHARGE')")
    public ResponseEntity<LabContinuousEvaluationResponse> update(@PathVariable Long id,
                                                                    @Valid @RequestBody LabContinuousEvaluationRequest request) {
        return ResponseEntity.ok(labContinuousEvaluationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        labContinuousEvaluationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
