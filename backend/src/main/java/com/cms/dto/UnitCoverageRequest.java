package com.cms.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

public record UnitCoverageRequest(
    @NotNull(message = "Unit ID is required")
    Long unitId,

    BigDecimal hoursCovered,

    Boolean markedComplete
) {}
