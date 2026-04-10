package com.cms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourseRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    String code,

    @NotNull(message = "Credits is required")
    @Min(value = 1, message = "Credits must be at least 1")
    @Max(value = 20, message = "Credits must not exceed 20")
    Integer credits,

    @NotNull(message = "Theory credits is required")
    @Min(value = 0, message = "Theory credits must be at least 0")
    @Max(value = 20, message = "Theory credits must not exceed 20")
    Integer theoryCredits,

    @NotNull(message = "Lab credits is required")
    @Min(value = 0, message = "Lab credits must be at least 0")
    @Max(value = 20, message = "Lab credits must not exceed 20")
    Integer labCredits,

    @NotNull(message = "Program ID is required")
    Long programId,

    @NotNull(message = "Semester is required")
    @Min(value = 1, message = "Semester must be at least 1")
    @Max(value = 12, message = "Semester must not exceed 12")
    Integer semester
) {}
