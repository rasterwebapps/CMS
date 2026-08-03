package com.cms.dto;

import java.time.Instant;

public record ClinicalVenueResponse(
    Long id,
    String name,
    String hospitalName,
    String department,
    Integer capacity,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt,
    Long roomId,
    String roomLabel
) {}
