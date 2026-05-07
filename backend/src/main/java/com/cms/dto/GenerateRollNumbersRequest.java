package com.cms.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;

import java.util.List;

/**
 * Request DTO for generating roll numbers for students.
 */
public record GenerateRollNumbersRequest(
        @NotEmpty(message = "Student IDs list cannot be empty")
        List<@NotNull Long> studentIds,

        @NotNull(message = "Course ID is required")
        Long courseId,

        @NotNull(message = "Academic year is required")
        @Min(value = 2000, message = "Academic year must be 2000 or later")
        Integer academicYear
) {
}

