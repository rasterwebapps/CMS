package com.cms.dto;

import java.time.Instant;

public record DesignationResponse(
    Long id,
    String name,
    String code,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
    public DesignationResponse(
        Long id,
        String name,
        String code,
        String description,
        Instant createdAt,
        Instant updatedAt
    ) {
        this(id, name, code, description, true, createdAt, updatedAt);
    }
}
