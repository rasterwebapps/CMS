package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaxRuleRequest(

    @NotNull(message = "Tax type is required")
    Long taxTypeId,

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0", message = "Rate cannot be negative")
    BigDecimal ratePercent,

    Boolean isActive
) {}
