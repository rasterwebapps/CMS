package com.cms.dto;

import java.math.BigDecimal;

public record FeeRefundSummaryResponse(
    Long id,
    String originalReceiptNumber,
    String studentName,
    String rollNumber,
    String admissionNumber,
    String programName,
    BigDecimal refundAmount,
    String reason,
    String requestedBy,
    String requestedAt,           // ISO-8601 instant string
    String status,                // PENDING | APPROVED | REJECTED
    // Set on APPROVED
    String refundNumber,
    String paymentMode,
    String paymentDate,           // ISO date string yyyy-MM-dd
    String transactionReference,
    String approvedBy,
    String approvedAt,            // ISO-8601 instant string
    // Set on REJECTED
    String rejectionReason
) {}
