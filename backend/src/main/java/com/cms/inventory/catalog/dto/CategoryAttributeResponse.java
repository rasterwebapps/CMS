package com.cms.inventory.catalog.dto;

import java.time.Instant;

public record CategoryAttributeResponse(
    Long id,
    Long categoryId,
    String name,
    String dataType,
    String enumOptions,
    Boolean isRequired,
    Integer displayOrder,
    Instant createdAt,
    Instant updatedAt
) {}
