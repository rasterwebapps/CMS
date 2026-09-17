package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.PenaltyResponse;
import com.cms.dto.ReceiptResponse;
import com.cms.dto.RazorpayOrderCreateRequest;
import com.cms.dto.RazorpayOrderResponse;
import com.cms.dto.StudentFeeAllocationResponse;
import com.cms.service.GuardianFeeSelfServiceService;
import com.cms.service.GuardianService;
import com.cms.service.RazorpayPaymentService;

import jakarta.validation.Valid;

/** Ward fee self-service for guardians -- every endpoint verifies ward ownership via
 *  {@link GuardianService#assertIsMyWard} first (403s on any student that isn't actually one of
 *  the caller's linked wards) before delegating to the same fee-domain services Student Portal and
 *  staff collection already use. Split from {@link GuardianController} to keep that one focused on
 *  Guardian record CRUD/linking, matching this codebase's per-domain controller shape. */
@RestController
@RequestMapping("/guardian/wards/{studentId}")
public class GuardianFeeController {

    private final GuardianService guardianService;
    private final GuardianFeeSelfServiceService guardianFeeSelfServiceService;
    private final RazorpayPaymentService razorpayPaymentService;

    public GuardianFeeController(GuardianService guardianService,
                                  GuardianFeeSelfServiceService guardianFeeSelfServiceService,
                                  RazorpayPaymentService razorpayPaymentService) {
        this.guardianService = guardianService;
        this.guardianFeeSelfServiceService = guardianFeeSelfServiceService;
        this.razorpayPaymentService = razorpayPaymentService;
    }

    @GetMapping("/fee-summary")
    @PreAuthorize("@perm.has('MY_WARD_FEE_VIEW')")
    public ResponseEntity<StudentFeeAllocationResponse> feeSummary(
            @PathVariable Long studentId, @AuthenticationPrincipal Jwt jwt) {
        guardianService.assertIsMyWard(username(jwt), studentId);
        return guardianFeeSelfServiceService.findWardSummary(studentId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/fee-receipts")
    @PreAuthorize("@perm.has('MY_WARD_FEE_VIEW')")
    public ResponseEntity<List<ReceiptResponse>> feeReceipts(
            @PathVariable Long studentId, @AuthenticationPrincipal Jwt jwt) {
        guardianService.assertIsMyWard(username(jwt), studentId);
        return ResponseEntity.ok(guardianFeeSelfServiceService.findWardReceipts(studentId));
    }

    @GetMapping("/fee-penalties")
    @PreAuthorize("@perm.has('MY_WARD_FEE_VIEW')")
    public ResponseEntity<PenaltyResponse> feePenalties(
            @PathVariable Long studentId, @AuthenticationPrincipal Jwt jwt) {
        guardianService.assertIsMyWard(username(jwt), studentId);
        return guardianFeeSelfServiceService.findWardPenalties(studentId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/fee-payments/orders")
    @PreAuthorize("@perm.has('MY_WARD_FEE_PAY')")
    public ResponseEntity<RazorpayOrderResponse> createFeePaymentOrder(
            @PathVariable Long studentId,
            @Valid @RequestBody RazorpayOrderCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = username(jwt);
        guardianService.assertIsMyWard(username, studentId);
        Long guardianId = guardianService.currentGuardianId(username);
        return ResponseEntity.ok(razorpayPaymentService.createOrder(studentId, guardianId, request.amount()));
    }

    private String username(Jwt jwt) {
        return jwt != null ? jwt.getClaimAsString("preferred_username") : "";
    }
}
