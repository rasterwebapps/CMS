package com.cms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ResponseEntity<List<UnifiedReceiptResponse>> getAllReceipts() {
        return ResponseEntity.ok(unifiedReceiptService.getAllReceipts());
    }

    @GetMapping("/{receiptNumber}")
    @PreAuthorize("@perm.has('RECEIPT_VIEW')")
    public ResponseEntity<UnifiedReceiptResponse> getReceipt(
            @PathVariable String receiptNumber) {
        return ResponseEntity.ok(unifiedReceiptService.getReceiptByNumber(receiptNumber));
    }
}

