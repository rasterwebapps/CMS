package com.cms.inventory.stock.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CycleCountAddLineRequest(

    @NotNull(message = "Product is required")
    Long productId,

    /** Optional — pins this line's count to one specific bin within the count's location
     *  instead of the whole location. See {@code CycleCountService.addLine}. */
    Long binId,

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    String notes
) {}
