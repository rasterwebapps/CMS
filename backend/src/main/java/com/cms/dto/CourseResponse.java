package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.DegreeType;

public record CourseResponse(
    Long id,
    String name,
    String code,
    DegreeType degreeType,
    Integer durationYears,
    ProgramResponse program,
    Instant createdAt,
    Instant updatedAt
) {}
