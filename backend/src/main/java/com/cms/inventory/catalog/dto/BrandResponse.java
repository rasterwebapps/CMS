package com.cms.inventory.catalog.dto;

import java.time.Instant;

public record BrandResponse(
    Long id,
    String name,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
