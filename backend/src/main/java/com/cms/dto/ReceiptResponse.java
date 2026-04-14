package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.PaymentMode;

public record ReceiptResponse(
    Long id,
    String receiptNumber,
    Long studentId,
    String studentName,
    String rollNumber,
    Long semesterFeeId,
    String semesterLabel,
    Integer yearNumber,
    BigDecimal amountPaid,
    LocalDate paymentDate,
    PaymentMode paymentMode,
    String transactionReference,
    String remarks,
    Instant createdAt
) {}
