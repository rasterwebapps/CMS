package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.DegreeType;

public record ProgramResponse(
    Long id,
    String name,
    String code,
    DegreeType degreeType,
    Integer durationYears,
    DepartmentResponse department,
    Instant createdAt,
    Instant updatedAt
) {}
