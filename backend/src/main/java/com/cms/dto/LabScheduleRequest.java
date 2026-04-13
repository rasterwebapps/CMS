package com.cms.dto;

import com.cms.model.enums.DayOfWeek;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LabScheduleRequest(
    @NotNull(message = "Lab ID is required")
    Long labId,

    @NotNull(message = "Course ID is required")
    Long courseId,

    @NotNull(message = "Faculty ID is required")
    Long facultyId,

    @NotNull(message = "Lab slot ID is required")
    Long labSlotId,

    @NotBlank(message = "Batch name is required")
    String batchName,

    @NotNull(message = "Day of week is required")
    DayOfWeek dayOfWeek,

    @NotNull(message = "Semester ID is required")
    Long semesterId,

    Boolean isActive
) {}
