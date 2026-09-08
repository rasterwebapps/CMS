package com.cms.inventory.stock.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CycleCountEnterCountRequest(

    @NotNull(message = "Counted quantity is required")
    @DecimalMin(value = "0", message = "Counted quantity cannot be negative")
    BigDecimal countedQty,

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    String notes
) {}
