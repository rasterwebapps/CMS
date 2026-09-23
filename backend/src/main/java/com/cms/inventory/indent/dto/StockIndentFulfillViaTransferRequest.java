package com.cms.inventory.indent.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockIndentFulfillViaTransferRequest(
    @NotNull(message = "Source location is required")
    Long sourceLocationId,

    @NotNull(message = "Transfer quantity is required")
    @DecimalMin(value = "0.001", message = "Transfer quantity must be greater than zero")
    BigDecimal transferQty,

    @Size(max = 500) String notes
) {}
