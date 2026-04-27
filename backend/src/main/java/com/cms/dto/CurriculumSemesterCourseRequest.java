package com.cms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CurriculumSemesterCourseRequest(
    @NotNull(message = "Curriculum version ID is required")
    Long curriculumVersionId,

    @NotNull(message = "Semester number is required")
    @Min(value = 1, message = "Semester number must be at least 1")
    Integer semesterNumber,

    @NotNull(message = "Subject ID is required")
    Long subjectId,

    Integer sortOrder
) {}
