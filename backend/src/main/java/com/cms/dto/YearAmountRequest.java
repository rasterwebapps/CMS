package com.cms.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record YearAmountRequest(
    @NotNull(message = "Year number is required")
    @Positive(message = "Year number must be positive")
    Integer yearNumber,

    @NotNull(message = "Year label is required")
    String yearLabel,

    @PositiveOrZero(message = "Amount must be zero or positive")
    BigDecimal amount
) {}
