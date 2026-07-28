package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.AttendanceType;

public record SyllabusUnitDto(
    Long id,
    Long curriculumTermCourseId,
    Integer unitNumber,
    String title,
    AttendanceType componentType,
    Integer plannedHours,
    String description,
    Integer sortOrder,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
