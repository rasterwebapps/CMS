package com.cms.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SemesterRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @NotNull(message = "Academic year ID is required")
    Long academicYearId,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    @NotNull(message = "End date is required")
    LocalDate endDate,

    @NotNull(message = "Semester number is required")
    @Min(value = 1, message = "Semester number must be at least 1")
    @Max(value = 8, message = "Semester number must not exceed 8")
    Integer semesterNumber
) {}
