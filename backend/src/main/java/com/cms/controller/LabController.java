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

import com.cms.dto.LabInChargeAssignmentRequest;
import com.cms.dto.LabInChargeAssignmentResponse;
import com.cms.dto.LabRequest;
import com.cms.dto.LabResponse;
import com.cms.service.LabService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/labs")
public class LabController {

    private final LabService labService;

    public LabController(LabService labService) {
        this.labService = labService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<LabResponse> create(@Valid @RequestBody LabRequest request) {
        LabResponse response = labService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<LabResponse>> findAll() {
        List<LabResponse> labs = labService.findAll();
        return ResponseEntity.ok(labs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LabResponse> findById(@PathVariable Long id) {
        LabResponse response = labService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<LabResponse>> findByDepartmentId(@PathVariable Long departmentId) {
        List<LabResponse> labs = labService.findByDepartmentId(departmentId);
        return ResponseEntity.ok(labs);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_LAB_INCHARGE')")
    public ResponseEntity<LabResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LabRequest request) {
        LabResponse response = labService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        labService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<LabInChargeAssignmentResponse> assignInCharge(
            @PathVariable Long id,
            @Valid @RequestBody LabInChargeAssignmentRequest request) {
        LabInChargeAssignmentResponse response = labService.assignInCharge(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{labId}/assignments/{assignmentId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> removeAssignment(
            @PathVariable Long labId,
            @PathVariable Long assignmentId) {
        labService.removeAssignment(labId, assignmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/assignments")
    public ResponseEntity<List<LabInChargeAssignmentResponse>> findAssignments(@PathVariable Long id) {
        List<LabInChargeAssignmentResponse> assignments = labService.findAssignmentsByLabId(id);
        return ResponseEntity.ok(assignments);
    }
}
