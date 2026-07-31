package com.cms.dto;

import java.time.Instant;

public record ClinicalVenueResponse(
    Long id,
    String name,
    String hospitalName,
    String department,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
