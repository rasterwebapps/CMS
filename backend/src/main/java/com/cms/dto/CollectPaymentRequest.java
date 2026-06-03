package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.PaymentMode;
import com.cms.validation.TransactionReferenceRequired;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@TransactionReferenceRequired
public record CollectPaymentRequest(
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    @NotNull(message = "Payment date is required")
    LocalDate paymentDate,

    @NotNull(message = "Payment mode is required")
    PaymentMode paymentMode,

    String transactionReference,

    String remarks,

    /** Explicit receipt number to use. Null = auto-generate from the payment date's year sequence. */
    String receiptNumber
) {}
