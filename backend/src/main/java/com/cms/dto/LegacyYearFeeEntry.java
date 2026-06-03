package com.cms.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record LegacyYearFeeEntry(
    @NotNull Integer yearNumber,
    @NotNull @PositiveOrZero BigDecimal totalFee
) {}
