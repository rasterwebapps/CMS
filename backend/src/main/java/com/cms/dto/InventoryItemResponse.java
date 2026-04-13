package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

public record InventoryItemResponse(
    Long id,
    String name,
    String itemCode,
    Long labId,
    String labName,
    Integer quantity,
    Integer minimumQuantity,
    String unit,
    String description,
    LocalDate lastRestocked,
    boolean lowStock,
    Instant createdAt,
    Instant updatedAt
) {}
