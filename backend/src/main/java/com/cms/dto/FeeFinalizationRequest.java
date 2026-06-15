package com.cms.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

public record FeeFinalizationRequest(
    @NotNull(message = "Total fee is required")
    BigDecimal totalFee,

    BigDecimal discountAmount,

    String discountReason,

    String yearWiseFees,

    String termWiseFees,

    /** Optional commission override set during fee finalization. When provided, replaces the auto-resolved commission. */
    BigDecimal commissionAmount
) {}
