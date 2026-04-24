package com.cms.controller;

import java.time.LocalDate;
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

import com.cms.dto.FeePaymentRequest;
import com.cms.dto.FeePaymentResponse;
import com.cms.dto.StudentFeeStatusResponse;
import com.cms.service.FeePaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/fee-payments")
public class FeePaymentController {

    private final FeePaymentService feePaymentService;

    public FeePaymentController(FeePaymentService feePaymentService) {
        this.feePaymentService = feePaymentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<FeePaymentResponse> create(@Valid @RequestBody FeePaymentRequest request) {
        FeePaymentResponse response = feePaymentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FeePaymentResponse>> findAll(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        List<FeePaymentResponse> payments;
        if (studentId != null) {
            payments = feePaymentService.findByStudentId(studentId);
        } else if (startDate != null && endDate != null) {
            payments = feePaymentService.findByDateRange(startDate, endDate);
        } else {
            payments = feePaymentService.findAll();
        }
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeePaymentResponse> findById(@PathVariable Long id) {
        FeePaymentResponse response = feePaymentService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/receipt/{receiptNumber}")
    public ResponseEntity<FeePaymentResponse> findByReceiptNumber(@PathVariable String receiptNumber) {
        FeePaymentResponse response = feePaymentService.findByReceiptNumber(receiptNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<StudentFeeStatusResponse> getStudentFeeStatus(
            @RequestParam Long studentId,
            @RequestParam Long academicYearId) {
        StudentFeeStatusResponse status = feePaymentService.getStudentFeeStatus(studentId, academicYearId);
        return ResponseEntity.ok(status);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<FeePaymentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FeePaymentRequest request) {
        FeePaymentResponse response = feePaymentService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        feePaymentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
