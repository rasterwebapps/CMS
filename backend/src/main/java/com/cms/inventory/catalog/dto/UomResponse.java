package com.cms.inventory.catalog.dto;

import java.time.Instant;

public record UomResponse(
    Long id,
    String code,
    String name,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
