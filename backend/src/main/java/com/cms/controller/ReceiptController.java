package com.cms.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.UnifiedReceiptResponse;
import com.cms.service.UnifiedReceiptService;

/**
 * Unified receipts endpoint — covers both student fee payments and
 * enquiry pre-enrollment payments in a single list.
 */
@RestController
@RequestMapping("/receipts")
public class ReceiptController {

    private final UnifiedReceiptService unifiedReceiptService;

    public ReceiptController(UnifiedReceiptService unifiedReceiptService) {
        this.unifiedReceiptService = unifiedReceiptService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('RECEIPT_VIEW')")
    public ResponseEntity<Page<UnifiedReceiptResponse>> getAllReceipts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) String payerType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 25, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(unifiedReceiptService.getPaymentsPage(search, paymentMode, payerType, fromDate, toDate, pageable));
    }

    @GetMapping("/{receiptNumber}")
    @PreAuthorize("@perm.has('RECEIPT_VIEW')")
    public ResponseEntity<UnifiedReceiptResponse> getReceipt(
            @PathVariable String receiptNumber) {
        return ResponseEntity.ok(unifiedReceiptService.getReceiptByNumber(receiptNumber));
    }
}

