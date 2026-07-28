package com.cms.dto;

import com.cms.model.enums.AttendanceType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SyllabusUnitRequest(
    @NotNull(message = "Curriculum term course ID is required")
    Long curriculumTermCourseId,

    @NotNull(message = "Unit number is required")
    @Min(value = 1, message = "Unit number must be at least 1")
    Integer unitNumber,

    @NotBlank(message = "Title is required")
    String title,

    @NotNull(message = "Component type is required")
    AttendanceType componentType,

    Integer plannedHours,

    String description,

    Integer sortOrder
) {}
