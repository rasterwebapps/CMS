package com.cms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.DisbursementResponse;
import com.cms.dto.ScholarshipApplicationRequest;
import com.cms.dto.ScholarshipApplicationResponse;
import com.cms.dto.ScholarshipEligibilityRequest;
import com.cms.dto.ScholarshipEligibilityResponse;
import com.cms.dto.ScholarshipTypeResponse;
import com.cms.dto.ScholarshipVerificationRequest;
import com.cms.service.ScholarshipDisbursementService;
import com.cms.service.StudentScholarshipEligibilityService;
import com.cms.service.StudentScholarshipService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/students/{studentId}")
public class StudentScholarshipController {

    private final StudentScholarshipService studentScholarshipService;
    private final StudentScholarshipEligibilityService eligibilityService;
    private final ScholarshipDisbursementService disbursementService;

    public StudentScholarshipController(StudentScholarshipService studentScholarshipService,
                                        StudentScholarshipEligibilityService eligibilityService,
                                        ScholarshipDisbursementService disbursementService) {
        this.studentScholarshipService = studentScholarshipService;
        this.eligibilityService = eligibilityService;
        this.disbursementService = disbursementService;
    }

    @GetMapping("/scholarships/eligible")
    @PreAuthorize("@perm.has('SCHOLARSHIP_VIEW')")
    public ResponseEntity<List<ScholarshipTypeResponse>> eligible(@PathVariable Long studentId) {
        return ResponseEntity.ok(studentScholarshipService.getEligibleScholarships(studentId));
    }

    @GetMapping("/scholarships")
    @PreAuthorize("@perm.has('SCHOLARSHIP_VIEW')")
    public ResponseEntity<List<ScholarshipApplicationResponse>> applications(@PathVariable Long studentId) {
        return ResponseEntity.ok(studentScholarshipService.getStudentScholarships(studentId));
    }

    @PostMapping("/scholarships/apply")
    @PreAuthorize("@perm.has('SCHOLARSHIP_APPLY')")
    public ResponseEntity<ScholarshipApplicationResponse> apply(
            @PathVariable Long studentId,
            @Valid @RequestBody ScholarshipApplicationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ScholarshipApplicationResponse response = studentScholarshipService.applyForScholarship(
            studentId, request, username(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/scholarships/disbursements")
    @PreAuthorize("@perm.has('SCHOLARSHIP_VIEW')")
    public ResponseEntity<List<DisbursementResponse>> disbursements(@PathVariable Long studentId) {
        return ResponseEntity.ok(disbursementService.getStudentDisbursementHistory(studentId));
    }

    @GetMapping("/eligibility")
    @PreAuthorize("@perm.has('SCHOLARSHIP_VIEW')")
    public ResponseEntity<ScholarshipEligibilityResponse> getEligibility(@PathVariable Long studentId) {
        return ResponseEntity.ok(eligibilityService.getEligibility(studentId));
    }

    @PutMapping("/eligibility")
    @PreAuthorize("@perm.has('SCHOLARSHIP_MANAGE')")
    public ResponseEntity<ScholarshipEligibilityResponse> updateEligibility(
            @PathVariable Long studentId,
            @Valid @RequestBody ScholarshipEligibilityRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(eligibilityService.updateEligibility(studentId, request, username(jwt)));
    }

    @PutMapping("/eligibility/verify")
    @PreAuthorize("@perm.has('SCHOLARSHIP_APPROVE')")
    public ResponseEntity<ScholarshipEligibilityResponse> verifyEligibility(
            @PathVariable Long studentId,
            @RequestBody(required = false) ScholarshipVerificationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String remarks = request != null ? request.remarks() : null;
        return ResponseEntity.ok(eligibilityService.verifyEligibility(studentId, username(jwt), remarks));
    }

    private static String username(Jwt jwt) {
        return jwt != null && jwt.getClaimAsString("preferred_username") != null
            ? jwt.getClaimAsString("preferred_username")
            : "system";
    }
}

