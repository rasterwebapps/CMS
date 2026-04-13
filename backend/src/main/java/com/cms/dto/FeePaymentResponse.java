package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.FeeType;
import com.cms.model.enums.PaymentMode;
import com.cms.model.enums.PaymentStatus;

public record FeePaymentResponse(
    Long id,
    Long studentId,
    String studentName,
    String rollNumber,
    Long feeStructureId,
    FeeType feeType,
    BigDecimal feeAmount,
    String receiptNumber,
    BigDecimal amountPaid,
    LocalDate paymentDate,
    PaymentMode paymentMode,
    PaymentStatus status,
    String transactionReference,
    String remarks,
    Instant createdAt,
    Instant updatedAt
) {}
