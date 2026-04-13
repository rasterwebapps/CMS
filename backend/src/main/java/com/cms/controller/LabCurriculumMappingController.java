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

import com.cms.dto.LabCurriculumMappingRequest;
import com.cms.dto.LabCurriculumMappingResponse;
import com.cms.model.enums.OutcomeType;
import com.cms.service.LabCurriculumMappingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/curriculum-mappings")
public class LabCurriculumMappingController {

    private final LabCurriculumMappingService mappingService;

    public LabCurriculumMappingController(LabCurriculumMappingService mappingService) {
        this.mappingService = mappingService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FACULTY')")
    public ResponseEntity<LabCurriculumMappingResponse> create(
            @Valid @RequestBody LabCurriculumMappingRequest request) {
        LabCurriculumMappingResponse response = mappingService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<LabCurriculumMappingResponse>> findAll(
            @RequestParam(required = false) Long experimentId,
            @RequestParam(required = false) OutcomeType outcomeType,
            @RequestParam(required = false) String outcomeCode) {
        List<LabCurriculumMappingResponse> mappings;
        if (experimentId != null && outcomeType != null) {
            mappings = mappingService.findByExperimentIdAndOutcomeType(experimentId, outcomeType);
        } else if (experimentId != null) {
            mappings = mappingService.findByExperimentId(experimentId);
        } else if (outcomeCode != null) {
            mappings = mappingService.findByOutcomeCode(outcomeCode);
        } else {
            mappings = mappingService.findAll();
        }
        return ResponseEntity.ok(mappings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LabCurriculumMappingResponse> findById(@PathVariable Long id) {
        LabCurriculumMappingResponse response = mappingService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_FACULTY')")
    public ResponseEntity<LabCurriculumMappingResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LabCurriculumMappingRequest request) {
        LabCurriculumMappingResponse response = mappingService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        mappingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
