package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReceiptResponse(
    Long id,
    String receiptNumber,
    Long studentId,
    String studentName,
    String rollNumber,
    Long installmentFeeId,
    String installmentLabel,
    Integer yearNumber,
    BigDecimal amountPaid,
    LocalDate paymentDate,
    String paymentMode,
    String transactionReference,
    String remarks,
    Instant createdAt,
    String receiptType,
    String originalReceiptNumber,
    /** TUITION_ONLY | TUITION_AND_HOSTEL — null for refund rows */
    String feeCategory,
    /** Full name of the staff member who actually collected/approved this transaction — null when
     *  not recorded (e.g. legacy receipts collected before this field was tracked). */
    String collectedBy
) {}
