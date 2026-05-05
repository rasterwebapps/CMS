package com.cms.dto;

import java.time.Instant;

public record BloodGroupResponse(
    Long id,
    String name,
    String code,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}

