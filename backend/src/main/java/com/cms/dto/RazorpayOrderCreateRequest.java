package com.cms.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Guardian-requested amount for a ward fee payment -- custom/partial amounts are allowed (up to
 *  the ward's currently collectible outstanding, validated server-side in
 *  {@link com.cms.service.RazorpayPaymentService}), never a full-balance-only default. */
public record RazorpayOrderCreateRequest(
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount
) {
}
