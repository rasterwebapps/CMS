package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.RoomPurposeCategoryCode;

public record RoomPurposeCategoryResponse(
    Long id,
    String name,
    RoomPurposeCategoryCode code,
    Boolean isResidential,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
