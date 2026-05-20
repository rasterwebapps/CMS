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
    BigDecimal amountPaid,
    LocalDate paymentDate,
    String paymentMode,
    String transactionReference,
    String remarks,
    String installmentsCovered,
    String collectedBy,
    /** TUITION_ONLY | TUITION_AND_HOSTEL — null for pre-enrollment receipts */
    String feeCategory,
    Instant createdAt
) {}
