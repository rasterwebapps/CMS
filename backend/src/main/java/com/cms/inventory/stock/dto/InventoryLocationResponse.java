package com.cms.inventory.stock.dto;

import java.time.Instant;

public record InventoryLocationResponse(
    Long id,
    Long roomId,
    String roomNumber,
    Long zoneId,
    String zoneName,
    String virtualName,
    String locationRole,
    String description,
    Long defaultSupplyingLocationId,
    String defaultSupplyingLocationVirtualName,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
