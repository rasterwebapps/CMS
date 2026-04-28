package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.PaymentMode;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record TermFeePaymentRequest(
    @NotNull Long feeDemandId,
    @NotNull LocalDate paymentDate,
    @NotNull @DecimalMin("0.01") BigDecimal amountPaid,
    @NotNull PaymentMode paymentMode,
    String remarks
) {}
