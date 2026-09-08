package com.cms.inventory.catalog.dto;

import java.time.Instant;

public record CategoryResponse(
    Long id,
    String name,
    Long parentCategoryId,
    String parentCategoryName,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
