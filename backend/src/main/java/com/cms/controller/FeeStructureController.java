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

import com.cms.dto.BulkFeeStructureRequest;
import com.cms.dto.FeeStructureRequest;
import com.cms.dto.FeeStructureResponse;
import com.cms.dto.GroupedFeeStructureResponse;
import com.cms.service.FeeStructureService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/fee-structures")
public class FeeStructureController {

    private final FeeStructureService feeStructureService;

    public FeeStructureController(FeeStructureService feeStructureService) {
        this.feeStructureService = feeStructureService;
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<List<FeeStructureResponse>> bulkCreate(@Valid @RequestBody BulkFeeStructureRequest request) {
        List<FeeStructureResponse> responses = feeStructureService.bulkCreate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @PutMapping("/bulk")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<List<FeeStructureResponse>> bulkUpdate(@Valid @RequestBody BulkFeeStructureRequest request) {
        List<FeeStructureResponse> responses = feeStructureService.bulkUpdate(request);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/grouped")
    public ResponseEntity<List<GroupedFeeStructureResponse>> findGrouped(
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long courseId) {
        List<GroupedFeeStructureResponse> grouped = feeStructureService.findGrouped(programId, academicYearId, courseId);
        return ResponseEntity.ok(grouped);
    }

    @DeleteMapping("/group")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<Void> deleteGroup(
            @RequestParam Long programId,
            @RequestParam Long academicYearId,
            @RequestParam(required = false) Long courseId) {
        feeStructureService.deleteGroup(programId, academicYearId, courseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<FeeStructureResponse> create(@Valid @RequestBody FeeStructureRequest request) {
        FeeStructureResponse response = feeStructureService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FeeStructureResponse>> findAll(
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long courseId) {
        List<FeeStructureResponse> feeStructures;
        if (programId != null && courseId != null) {
            feeStructures = feeStructureService.findByProgramIdAndCourseId(programId, courseId);
        } else if (programId != null && academicYearId != null) {
            feeStructures = feeStructureService.findByProgramIdAndAcademicYearId(programId, academicYearId);
        } else if (programId != null) {
            feeStructures = feeStructureService.findByProgramId(programId);
        } else {
            feeStructures = feeStructureService.findAll();
        }
        return ResponseEntity.ok(feeStructures);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeeStructureResponse> findById(@PathVariable Long id) {
        FeeStructureResponse response = feeStructureService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<FeeStructureResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FeeStructureRequest request) {
        FeeStructureResponse response = feeStructureService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        feeStructureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
