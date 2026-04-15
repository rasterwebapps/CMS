package com.cms.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

public record FeeFinalizationRequest(
    @NotNull(message = "Total fee is required")
    BigDecimal totalFee,

    BigDecimal discountAmount,

    String discountReason,

    String yearWiseFees
) {}
