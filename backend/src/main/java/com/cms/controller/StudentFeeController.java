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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.CollectPaymentRequest;
import com.cms.dto.CollectPaymentResponse;
import com.cms.dto.FeeExplorerResponse;
import com.cms.dto.PenaltyResponse;
import com.cms.dto.ReceiptResponse;
import com.cms.dto.StudentFeeAllocationRequest;
import com.cms.dto.StudentFeeAllocationResponse;
import com.cms.service.FeeExplorerService;
import com.cms.service.FeeFinalizationService;
import com.cms.service.PaymentCollectionService;
import com.cms.service.PenaltyCalculationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/student-fees")
public class StudentFeeController {

    private final FeeFinalizationService feeFinalizationService;
    private final PaymentCollectionService paymentCollectionService;
    private final PenaltyCalculationService penaltyCalculationService;
    private final FeeExplorerService feeExplorerService;

    public StudentFeeController(FeeFinalizationService feeFinalizationService,
                                 PaymentCollectionService paymentCollectionService,
                                 PenaltyCalculationService penaltyCalculationService,
                                 FeeExplorerService feeExplorerService) {
        this.feeFinalizationService = feeFinalizationService;
        this.paymentCollectionService = paymentCollectionService;
        this.penaltyCalculationService = penaltyCalculationService;
        this.feeExplorerService = feeExplorerService;
    }

    @PostMapping("/finalize")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<StudentFeeAllocationResponse> finalize(
            @Valid @RequestBody StudentFeeAllocationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getClaimAsString("preferred_username") : "admin";
        StudentFeeAllocationResponse response = feeFinalizationService.finalize(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{studentId}/semester-breakdown")
    public ResponseEntity<StudentFeeAllocationResponse> getSemesterBreakdown(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(feeFinalizationService.getByStudentId(studentId));
    }

    @GetMapping("/{studentId}/semester-status")
    public ResponseEntity<StudentFeeAllocationResponse> getSemesterStatus(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(feeFinalizationService.getByStudentId(studentId));
    }

    @PostMapping("/{studentId}/collect")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN') or hasRole('ROLE_CASHIER')")
    public ResponseEntity<CollectPaymentResponse> collectPayment(
            @PathVariable Long studentId,
            @Valid @RequestBody CollectPaymentRequest request) {
        CollectPaymentResponse response = paymentCollectionService.collectPayment(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{studentId}/penalties")
    public ResponseEntity<PenaltyResponse> getPenalties(@PathVariable Long studentId) {
        PenaltyResponse response = penaltyCalculationService.calculatePenalties(studentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/explorer")
    public ResponseEntity<FeeExplorerResponse> explorer(
            @RequestParam(required = false) String search) {
        FeeExplorerResponse response = feeExplorerService.search(search);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{studentId}/receipts")
    public ResponseEntity<List<ReceiptResponse>> getReceipts(@PathVariable Long studentId) {
        List<ReceiptResponse> receipts = paymentCollectionService.getReceipts(studentId);
        return ResponseEntity.ok(receipts);
    }

    @GetMapping("/{studentId}/receipts/{receiptId}")
    public ResponseEntity<ReceiptResponse> getReceipt(
            @PathVariable Long studentId,
            @PathVariable Long receiptId) {
        ReceiptResponse receipt = paymentCollectionService.getReceiptById(studentId, receiptId);
        return ResponseEntity.ok(receipt);
    }
}
