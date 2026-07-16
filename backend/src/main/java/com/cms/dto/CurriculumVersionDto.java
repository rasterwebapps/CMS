package com.cms.dto;

import java.time.Instant;

public record CurriculumVersionDto(
    Long id,
    Long programId,
    String programName,
    Long courseId,
    String courseName,
    String versionName,
    Long effectiveFromAcademicYearId,
    String effectiveFromAcademicYearName,
    Boolean isActive,
    Integer termCount,
    Integer subjectCount,
    Boolean deletable,
    Instant createdAt,
    Instant updatedAt
) {}
