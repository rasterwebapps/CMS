package com.cms.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record YearAmountRequest(
    @NotNull(message = "Year number is required")
    @Positive(message = "Year number must be positive")
    Integer yearNumber,

    @NotNull(message = "Year label is required")
    String yearLabel,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount
) {}
