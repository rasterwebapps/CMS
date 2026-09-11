package com.cms.inventory.procurement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CurrencyExchangeRateRequest(

    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    String currencyCode,

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.000001", message = "Rate must be greater than zero")
    BigDecimal rateToBase,

    @NotNull(message = "Effective date is required")
    LocalDate effectiveDate,

    Boolean isActive
) {}
