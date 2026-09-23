package com.cms.inventory.indent.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockIndentRaisePoRequest(
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.001", message = "Quantity must be greater than zero")
    BigDecimal qty,

    @Size(max = 500) String notes
) {}
