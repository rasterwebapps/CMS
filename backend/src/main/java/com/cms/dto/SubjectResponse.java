package com.cms.dto;

import java.time.Instant;

public record SubjectResponse(
    Long id,
    String name,
    String code,
    Integer credits,
    Integer theoryCredits,
    Integer labCredits,
    CourseResponse course,
    SpecialityResponse speciality,
    Integer termNumber,
    Instant createdAt,
    Instant updatedAt
) {}
