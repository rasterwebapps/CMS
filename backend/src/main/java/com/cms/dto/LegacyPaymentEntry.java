package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.PaymentMode;
import com.cms.validation.TransactionReferenceRequired;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@TransactionReferenceRequired
public record LegacyPaymentEntry(
    @NotNull Integer yearNumber,
    @NotNull Integer semesterSequence,
    @NotNull LocalDate paymentDate,
    @NotNull @Positive BigDecimal amount,
    @NotNull PaymentMode paymentMode,
    /** Existing receipt number from physical records. Null = auto-generate from payment date's year. */
    String receiptNumber,
    String transactionReference,
    String remarks
) {}
