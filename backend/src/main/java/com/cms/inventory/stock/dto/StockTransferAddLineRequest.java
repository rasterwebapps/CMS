package com.cms.inventory.stock.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockTransferAddLineRequest(
    @NotNull Long productId,
    @NotNull @DecimalMin(value = "0.001", message = "Quantity must be greater than zero") BigDecimal quantity,
    @Size(max = 500) String notes
) {}
