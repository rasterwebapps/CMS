package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.ProgramStatus;

public record ProgramResponse(
    Long id,
    String name,
    String code,
    Integer durationYears,
    Integer totalSemesters,
    ProgramStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
