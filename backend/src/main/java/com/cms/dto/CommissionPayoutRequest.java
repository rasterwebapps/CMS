package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CommissionPayoutRequest(
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    BigDecimal amount,

    @NotNull(message = "Payout date is required")
    LocalDate payoutDate,

    @NotNull(message = "Payment mode is required")
    String paymentMode,

    String transactionReference,
    String remarks
) {}
