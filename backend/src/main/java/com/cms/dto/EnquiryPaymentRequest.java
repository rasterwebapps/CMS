package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.PaymentMode;
import com.cms.validation.TransactionReferenceRequired;

import jakarta.validation.constraints.NotNull;

@TransactionReferenceRequired
public record EnquiryPaymentRequest(
    @NotNull BigDecimal amountPaid,
    @NotNull LocalDate paymentDate,
    @NotNull PaymentMode paymentMode,
    String transactionReference,
    String remarks
) {}
