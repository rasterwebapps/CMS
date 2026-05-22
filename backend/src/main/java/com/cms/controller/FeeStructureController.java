package com.cms.controller;

import java.math.BigDecimal;
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
import com.cms.dto.FeeGuidelineResponse;
import com.cms.dto.FeeStructureRequest;
import com.cms.dto.FeeStructureResponse;
import com.cms.dto.GroupedFeeStructureResponse;
import com.cms.model.enums.AdmissionQuota;
import com.cms.model.enums.Gender;
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
    @PreAuthorize("@perm.has('FEE_STRUCTURE_MANAGE')")
    public ResponseEntity<List<FeeStructureResponse>> bulkCreate(@Valid @RequestBody BulkFeeStructureRequest request) {
        List<FeeStructureResponse> responses = feeStructureService.bulkCreate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @PutMapping("/bulk")
    @PreAuthorize("@perm.has('FEE_STRUCTURE_MANAGE')")
    public ResponseEntity<List<FeeStructureResponse>> bulkUpdate(@Valid @RequestBody BulkFeeStructureRequest request) {
        List<FeeStructureResponse> responses = feeStructureService.bulkUpdate(request);
        return ResponseEntity.ok(responses);
    }

    /**
     * Fee guideline for the enquiry form — returns the items for a specific
     * (program, course, quota, feeState, gender) combination with
     * fallback to Other State if no exact match exists.
     * Returns 404 when no fee structure is configured (after fallback).
     */
    @GetMapping("/guideline")
    public ResponseEntity<FeeGuidelineResponse> getFeeGuideline(
            @RequestParam Long programId,
            @RequestParam(required = false) Long courseId,
            @RequestParam AdmissionQuota quota,
            @RequestParam Long feeStateId,
            @RequestParam Gender gender) {
        return feeStructureService.findForEnquiry(programId, courseId, quota, feeStateId, gender)
            .map(items -> {
                BigDecimal total = items.stream()
                    .map(FeeStructureResponse::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                return ResponseEntity.ok(new FeeGuidelineResponse(total, items));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/grouped")
    public ResponseEntity<List<GroupedFeeStructureResponse>> findGrouped(
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) AdmissionQuota quota,
            @RequestParam(required = false) Long feeStateId,
            @RequestParam(required = false) Gender gender) {
        List<GroupedFeeStructureResponse> grouped =
            feeStructureService.findGrouped(programId, academicYearId, courseId, quota, feeStateId, gender);
        return ResponseEntity.ok(grouped);
    }

    @DeleteMapping("/group")
    @PreAuthorize("@perm.has('FEE_STRUCTURE_MANAGE')")
    public ResponseEntity<Void> deleteGroup(
            @RequestParam Long programId,
            @RequestParam Long academicYearId,
            @RequestParam(required = false) Long courseId,
            @RequestParam AdmissionQuota quota,
            @RequestParam Long feeStateId,
            @RequestParam Gender gender) {
        feeStructureService.deleteGroup(programId, academicYearId, courseId, quota, feeStateId, gender);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("@perm.has('FEE_STRUCTURE_MANAGE')")
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
        if (programId != null && courseId != null && academicYearId != null) {
            feeStructures = feeStructureService.findByProgramIdAndCourseIdAndAcademicYearId(programId, courseId, academicYearId);
        } else if (programId != null && courseId != null) {
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
    @PreAuthorize("@perm.has('FEE_STRUCTURE_MANAGE')")
    public ResponseEntity<FeeStructureResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FeeStructureRequest request) {
        FeeStructureResponse response = feeStructureService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('FEE_STRUCTURE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        feeStructureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
