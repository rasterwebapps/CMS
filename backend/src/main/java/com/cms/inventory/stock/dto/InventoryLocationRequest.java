package com.cms.inventory.stock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryLocationRequest(

    @NotNull(message = "Room is required")
    Long roomId,

    @NotBlank(message = "Virtual name is required")
    @Size(max = 150, message = "Virtual name must not exceed 150 characters")
    String virtualName,

    @NotBlank(message = "Location role is required")
    String locationRole,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    Boolean isActive
) {}
