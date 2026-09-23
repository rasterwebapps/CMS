package com.cms.inventory.indent.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockIndentReturnLineRequest(
    @NotNull @DecimalMin(value = "0.001", message = "Returned quantity must be greater than zero") BigDecimal returnedQty,
    @Size(max = 500) String notes
) {}
