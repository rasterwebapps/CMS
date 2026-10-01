package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.PaymentMode;

public record PaymentRowDto(
    Long id,
    LocalDate paymentDate,
    BigDecimal amountPaid,
    BigDecimal lateFeeApplied,
    BigDecimal totalCollected,
    PaymentMode paymentMode,
    String receiptNumber,
    String transactionReference,
    String remarks,
    String feeCategory,
    /** Full name of the staff member who actually collected this payment — null when not
     *  recorded (e.g. legacy receipts collected before this field was tracked). */
    String collectedBy,
    /** Moment the record was created — used as the payment's clock-time on the printed receipt. */
    Instant createdAt
) {}
