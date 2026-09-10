package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaxSubTypeRequest(

    @NotNull(message = "Tax is required")
    Long taxRuleId,

    @NotBlank(message = "Jurisdiction mode is required")
    String jurisdictionMode,

    @NotBlank(message = "Component name is required")
    @Size(max = 50, message = "Component name must not exceed 50 characters")
    String componentName,

    @NotNull(message = "Split % is required")
    @DecimalMin(value = "0.01", message = "Split % must be greater than 0")
    @DecimalMax(value = "100", message = "Split % cannot exceed 100")
    BigDecimal splitPercent,

    Boolean isActive
) {}
