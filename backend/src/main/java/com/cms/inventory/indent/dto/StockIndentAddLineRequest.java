package com.cms.inventory.indent.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockIndentAddLineRequest(
    @NotNull Long productId,
    /** Required once the product has any active {@code ProductVariant}. */
    Long variantId,
    @NotNull @DecimalMin(value = "0.001", message = "Requested quantity must be greater than zero") BigDecimal requestedQty,
    @Size(max = 500) String notes
) {}
