package com.cms.inventory.stock.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ProductLocationReorderConfigRequest(

    @NotNull(message = "Product is required")
    Long productId,

    @NotNull(message = "Location is required")
    Long locationId,

    @NotNull(message = "Reorder level is required")
    @DecimalMin(value = "0", message = "Reorder level cannot be negative")
    BigDecimal reorderLevel,

    @NotNull(message = "Reorder quantity is required")
    @DecimalMin(value = "0.001", message = "Reorder quantity must be greater than zero")
    BigDecimal reorderQty,

    BigDecimal maxStockQty,

    Boolean autoIndentEnabled,

    Boolean isActive
) {}
