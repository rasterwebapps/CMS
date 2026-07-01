package com.cms.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.DisbursementRequest;
import com.cms.dto.DisbursementResponse;
import com.cms.dto.ScholarshipApplicationResponse;
import com.cms.dto.ScholarshipApprovalRequest;
import com.cms.dto.ScholarshipRejectionRequest;
import com.cms.dto.ScholarshipSanctionRequest;
import com.cms.model.OneBookPaymentRequest;
import com.cms.service.OneBookIntegrationService;
import com.cms.service.ScholarshipDisbursementService;
import com.cms.service.StudentScholarshipService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/scholarship-applications")
public class ScholarshipApplicationController {

    private final StudentScholarshipService studentScholarshipService;
    private final ScholarshipDisbursementService disbursementService;
    private final OneBookIntegrationService oneBookService;

    public ScholarshipApplicationController(StudentScholarshipService studentScholarshipService,
                                            ScholarshipDisbursementService disbursementService,
                                            OneBookIntegrationService oneBookService) {
        this.studentScholarshipService = studentScholarshipService;
        this.disbursementService = disbursementService;
        this.oneBookService = oneBookService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('SCHOLARSHIP_APPROVE')")
    public ResponseEntity<Page<ScholarshipApplicationResponse>> pending(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "applicationDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(studentScholarshipService.getPendingApplicationsPage(search, pageable));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("@perm.has('SCHOLARSHIP_APPROVE')")
    public ResponseEntity<ScholarshipApplicationResponse> approve(
            @PathVariable Long id,
            @Valid @RequestBody ScholarshipApprovalRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(studentScholarshipService.approveScholarship(id, request, username(jwt)));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("@perm.has('SCHOLARSHIP_APPROVE')")
    public ResponseEntity<ScholarshipApplicationResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody ScholarshipRejectionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(studentScholarshipService.rejectScholarship(id, request, username(jwt)));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("@perm.has('SCHOLARSHIP_MANAGE')")
    public ResponseEntity<ScholarshipApplicationResponse> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(studentScholarshipService.cancelScholarship(id, username(jwt)));
    }

    /**
     * Records the govt sanction for an APPROVED govt-portal scholarship.
     * Status moves APPROVED → SANCTIONED.
     */
    @PutMapping("/{id}/sanction")
    @PreAuthorize("@perm.has('SCHOLARSHIP_APPROVE')")
    public ResponseEntity<ScholarshipApplicationResponse> sanction(
            @PathVariable Long id,
            @Valid @RequestBody ScholarshipSanctionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(studentScholarshipService.sanctionScholarship(id, request, username(jwt)));
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize("@perm.has('SCHOLARSHIP_APPLY')")
    public ResponseEntity<ScholarshipApplicationResponse> renew(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        ScholarshipApplicationResponse response = studentScholarshipService.renewScholarship(id, username(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/disburse")
    @PreAuthorize("@perm.has('SCHOLARSHIP_DISBURSE')")
    public ResponseEntity<DisbursementResponse> disburse(
            @PathVariable Long id,
            @Valid @RequestBody DisbursementRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        DisbursementResponse response = disbursementService.disburse(id, request, username(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/disburse-onebook")
    @PreAuthorize("@perm.has('SCHOLARSHIP_DISBURSE')")
    public ResponseEntity<Map<String, String>> disburseViaOneBook(
            @PathVariable Long id,
            @Valid @RequestBody DisbursementRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        OneBookPaymentRequest obReq = oneBookService.pushScholarshipPayment(id, request, username(jwt));
        return ResponseEntity.ok(Map.of("referenceId", obReq.getReferenceId(), "status", obReq.getStatus()));
    }

    @GetMapping("/{id}/disbursements")
    @PreAuthorize("@perm.has('SCHOLARSHIP_VIEW')")
    public ResponseEntity<List<DisbursementResponse>> disbursements(@PathVariable Long id) {
        return ResponseEntity.ok(disbursementService.getApplicationDisbursements(id));
    }

    private static String username(Jwt jwt) {
        return jwt != null && jwt.getClaimAsString("preferred_username") != null
            ? jwt.getClaimAsString("preferred_username")
            : "system";
    }
}

