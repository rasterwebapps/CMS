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

package com.cms.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GenerateRollNumbersRequest(
    @NotNull Long courseId,
    @NotEmpty List<Long> studentIds,
    @NotNull @Positive Integer academicYear
) {}
