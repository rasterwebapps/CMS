package com.cms.dto;

import java.time.Instant;

public record SubjectResponse(
    Long id,
    String name,
    String code,
    Integer credits,
    Integer theoryCredits,
    Integer labCredits,
    SpecialityResponse speciality,
    Integer termNumber,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
