package com.cms.dto;

import java.time.Instant;

public record BatchDto(
    Long id,
    Long courseOfferingId,
    String name,
    Integer capacity,
    long enrolledCount,
    Long termInstanceId,
    Long coordinatorFacultyId,
    String coordinatorFacultyName,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
