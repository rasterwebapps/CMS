package com.cms.dto;

import java.time.Instant;

public record DesignationResponse(
    Long id,
    String name,
    String code,
    String description,
    Instant createdAt,
    Instant updatedAt
) {}
