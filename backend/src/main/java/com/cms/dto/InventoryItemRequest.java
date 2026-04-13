package com.cms.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record InventoryItemRequest(
    @NotBlank(message = "Name is required")
    String name,

    String itemCode,

    @NotNull(message = "Lab ID is required")
    Long labId,

    @NotNull(message = "Quantity is required")
    @PositiveOrZero(message = "Quantity must be zero or positive")
    Integer quantity,

    @PositiveOrZero(message = "Minimum quantity must be zero or positive")
    Integer minimumQuantity,

    String unit,

    String description,

    LocalDate lastRestocked
) {}
