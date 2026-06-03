package com.cms.dto;

import java.time.Instant;

public record SpecialityResponse(
    Long id,
    String name,
    String code,
    String description,
    Long hodFacultyId,
    String hodName,
    Instant createdAt,
    Instant updatedAt
) {}
