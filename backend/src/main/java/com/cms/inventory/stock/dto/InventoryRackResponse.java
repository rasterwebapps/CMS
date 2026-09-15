package com.cms.inventory.stock.dto;

import java.time.Instant;

public record InventoryRackResponse(
    Long id,
    Long locationId,
    String locationName,
    String name,
    String code,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
