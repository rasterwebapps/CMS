package com.cms.dto;

import java.time.Instant;

public record InstitutionResponse(
    Long id,
    String name,
    String code,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
