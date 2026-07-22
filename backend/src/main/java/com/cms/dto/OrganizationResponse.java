package com.cms.dto;

import java.time.Instant;

public record OrganizationResponse(
    Long id,
    String name,
    String code,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
