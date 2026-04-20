package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.PaymentMode;

import jakarta.validation.constraints.NotNull;

public record EnquiryPaymentRequest(
    @NotNull BigDecimal amountPaid,
    @NotNull LocalDate paymentDate,
    @NotNull PaymentMode paymentMode,
    String transactionReference,
    String remarks
) {}
