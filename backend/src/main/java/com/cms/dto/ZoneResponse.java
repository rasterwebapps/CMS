package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.GenderRestriction;

public record ZoneResponse(
    Long id,
    String name,
    Boolean isHostel,
    GenderRestriction genderRestriction,
    Long wardenId,
    String wardenName,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt,
    Long floorId,
    String floorName
) {}
