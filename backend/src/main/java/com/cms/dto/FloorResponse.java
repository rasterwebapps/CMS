package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.GenderRestriction;

public record FloorResponse(
    Long id,
    String name,
    Integer floorNumber,
    Boolean isHostel,
    GenderRestriction genderRestriction,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt,
    Long blockId,
    String blockName
) {}
