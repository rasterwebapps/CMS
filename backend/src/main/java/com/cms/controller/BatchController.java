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

import com.cms.dto.BatchDto;
import com.cms.dto.BatchRequest;
import com.cms.dto.BatchStudentDto;
import com.cms.service.BatchService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/batches")
public class BatchController {

    private final BatchService service;

    public BatchController(BatchService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("@perm.has('BATCH_MANAGE')")
    public ResponseEntity<BatchDto> createBatch(@Valid @RequestBody BatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createBatch(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('BATCH_MANAGE')")
    public ResponseEntity<BatchDto> updateBatch(@PathVariable Long id, @Valid @RequestBody BatchRequest request) {
        return ResponseEntity.ok(service.updateBatch(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('BATCH_MANAGE')")
    public ResponseEntity<Void> deactivateBatch(@PathVariable Long id) {
        service.deactivateBatch(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("@perm.has('BATCH_VIEW')")
    public ResponseEntity<List<BatchDto>> getBatches(
            @RequestParam(required = false) Long courseOfferingId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long termInstanceId) {
        if (courseOfferingId != null) {
            return ResponseEntity.ok(service.getBatchesForOffering(courseOfferingId));
        }
        if (subjectId != null && termInstanceId != null) {
            return ResponseEntity.ok(service.getBatchesForSubjectAndTerm(subjectId, termInstanceId));
        }
        throw new IllegalArgumentException("Either courseOfferingId or subjectId+termInstanceId is required");
    }

    @GetMapping("/{id}/roster")
    @PreAuthorize("@perm.has('BATCH_VIEW')")
    public ResponseEntity<List<BatchStudentDto>> getRoster(@PathVariable Long id) {
        return ResponseEntity.ok(service.getRoster(id));
    }

    @PostMapping("/{id}/students/{studentId}")
    @PreAuthorize("@perm.has('BATCH_MANAGE')")
    public ResponseEntity<Void> addStudent(@PathVariable Long id, @PathVariable Long studentId) {
        service.addStudent(id, studentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/students/{studentId}")
    @PreAuthorize("@perm.has('BATCH_MANAGE')")
    public ResponseEntity<Void> removeStudent(@PathVariable Long id, @PathVariable Long studentId) {
        service.removeStudent(id, studentId);
        return ResponseEntity.noContent().build();
    }
}
