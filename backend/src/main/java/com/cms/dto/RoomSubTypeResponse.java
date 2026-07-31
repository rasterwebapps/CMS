package com.cms.dto;

import java.time.Instant;

public record RoomSubTypeResponse(
    Long id,
    Long purposeCategoryId,
    String purposeCategoryName,
    String name,
    String code,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
