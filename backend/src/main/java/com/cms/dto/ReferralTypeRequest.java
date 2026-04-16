package com.cms.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReferralTypeRequest(
    @NotBlank(message = "Name is required")
    String name,

    @NotBlank(message = "Code is required")
    String code,

    @NotNull(message = "Commission amount is required")
    BigDecimal commissionAmount,

    Boolean hasCommission,

    String description,

    Boolean isActive
) {}
