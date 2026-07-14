package com.cms.dto;

import com.cms.model.enums.SubjectType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CurriculumSemesterCourseRequest(
    @NotNull(message = "Curriculum version ID is required")
    Long curriculumVersionId,

    @NotNull(message = "Term number is required")
    @Min(value = 1, message = "Term number must be at least 1")
    Integer termNumber,

    @NotNull(message = "Subject ID is required")
    Long subjectId,

    Integer sortOrder,

    @Min(value = 0, message = "Theory hours cannot be negative")
    Integer theoryHours,

    @Min(value = 0, message = "Lab hours cannot be negative")
    Integer labHours,

    @Min(value = 0, message = "Clinical hours cannot be negative")
    Integer clinicalHours,

    SubjectType subjectType,

    Boolean isElective,

    Long electiveGroupId,

    /** Optional: restrict this row to one specific course under the curriculum's program. */
    Long courseId
) {}
