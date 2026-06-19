 package com.cms.dto;

import java.time.Instant;

public record SpecialityResponse(
    Long id,
    String name,
    String code,
    String description,
    Long hodFacultyId,
    String hodName,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
    public SpecialityResponse(
        Long id,
        String name,
        String code,
        String description,
        Long hodFacultyId,
        String hodName,
        Instant createdAt,
        Instant updatedAt
    ) {
        this(id, name, code, description, hodFacultyId, hodName, true, createdAt, updatedAt);
    }
}
