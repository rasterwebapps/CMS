package com.cms.inventory.stock.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CycleCountAddLineRequest(

    @NotNull(message = "Product is required")
    Long productId,

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    String notes
) {}
