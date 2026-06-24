package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record UnifiedReceiptResponse(
    Long id,
    String receiptNumber,
    String payerType,           // STUDENT | ENQUIRY
    Long payerId,
    String payerName,
    String payerIdentifier,     // roll number for students, null for enquiries
    String admissionNumber,
    String programName,
    Long academicYearId,
    String academicYearName,
    BigDecimal amountPaid,
    LocalDate paymentDate,
    String paymentMode,
    String transactionReference,
    String remarks,
    String installmentsCovered,
    String collectedBy,
    /** TUITION_ONLY | TUITION_AND_HOSTEL — null for pre-enrollment receipts */
    String feeCategory,
    Instant createdAt,
    /** PAYMENT | REFUND — distinguishes original receipts from reversal records */
    String receiptType,
    /** True when the original payment receipt has already been refunded (APPROVED). */
    boolean refunded,
    /** PENDING | APPROVED for payment receipts with active refund workflow; otherwise null. */
    String refundStatus
) {}
