package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.GenderRestriction;

public record BlockResponse(
    Long id,
    String name,
    String code,
    String description,
    Boolean isHostel,
    GenderRestriction genderRestriction,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt,
    Long branchId,
    String branchName
) {}
