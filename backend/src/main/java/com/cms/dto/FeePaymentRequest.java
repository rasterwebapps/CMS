package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.PaymentMode;
import com.cms.model.enums.PaymentStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FeePaymentRequest(
    @NotNull(message = "Student ID is required")
    Long studentId,

    @NotNull(message = "Fee Structure ID is required")
    Long feeStructureId,

    @NotNull(message = "Amount paid is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amountPaid,

    @NotNull(message = "Payment date is required")
    LocalDate paymentDate,

    @NotNull(message = "Payment mode is required")
    PaymentMode paymentMode,

    @NotNull(message = "Status is required")
    PaymentStatus status,

    String transactionReference,

    String remarks
) {}
