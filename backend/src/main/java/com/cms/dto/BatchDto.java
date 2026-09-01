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
    Long labId,
    String labName,
    Long clinicalVenueId,
    String clinicalVenueName,
    Long clinicalShiftGroupId,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
