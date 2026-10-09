package com.cms.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** One term's admin-edited fee amount, submitted as part of a boarding status switch. */
public record TermFeeOverrideInput(
    @NotNull(message = "Semester number is required")
    Integer semesterNumber,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    BigDecimal amount
) {}
