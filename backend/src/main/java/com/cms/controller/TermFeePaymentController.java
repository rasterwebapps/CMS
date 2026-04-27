package com.cms.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.TermFeePaymentDto;
import com.cms.dto.TermFeePaymentRequest;
import com.cms.service.TermFeePaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/term-fee-payments")
public class TermFeePaymentController {

    private final TermFeePaymentService termFeePaymentService;

    public TermFeePaymentController(TermFeePaymentService termFeePaymentService) {
        this.termFeePaymentService = termFeePaymentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_COLLEGE_ADMIN') or hasRole('ROLE_CASHIER')")
    public ResponseEntity<TermFeePaymentDto> recordPayment(
            @Valid @RequestBody TermFeePaymentRequest request) {
        TermFeePaymentDto dto = termFeePaymentService.recordPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public ResponseEntity<List<TermFeePaymentDto>> getPayments(
            @RequestParam(required = false) Long demandId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (demandId != null) {
            return ResponseEntity.ok(termFeePaymentService.getPaymentsByDemand(demandId));
        }
        if (from != null && to != null) {
            return ResponseEntity.ok(termFeePaymentService.getPaymentsByDateRange(from, to));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TermFeePaymentDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(termFeePaymentService.getById(id));
    }

    @GetMapping("/receipt/{receiptNumber}")
    public ResponseEntity<TermFeePaymentDto> getByReceipt(@PathVariable String receiptNumber) {
        return ResponseEntity.ok(termFeePaymentService.getPaymentByReceipt(receiptNumber));
    }
}
