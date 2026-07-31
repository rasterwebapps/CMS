package com.cms.dto;

import java.time.Instant;

public record RoomPurposeCategoryResponse(
    Long id,
    String name,
    String code,
    Boolean isResidential,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
