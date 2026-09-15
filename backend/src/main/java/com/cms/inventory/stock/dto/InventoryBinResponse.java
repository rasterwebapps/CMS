package com.cms.inventory.stock.dto;

import java.time.Instant;

public record InventoryBinResponse(
    Long id,
    Long rackId,
    String rackName,
    Long locationId,
    String locationName,
    String name,
    String code,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
