package com.cms.dto;

import com.cms.model.enums.DayOfWeek;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LabScheduleRequest(
    @NotNull(message = "Lab ID is required")
    Long labId,

    @NotNull(message = "Subject ID is required")
    Long subjectId,

    @NotNull(message = "Faculty ID is required")
    Long facultyId,

    @NotNull(message = "Lab slot ID is required")
    Long labSlotId,

    @NotBlank(message = "Batch name is required")
    String batchName,

    @NotNull(message = "Day of week is required")
    DayOfWeek dayOfWeek,

    @NotNull(message = "Term instance ID is required")
    Long termInstanceId,

    Boolean isActive,

    /** Optional real Batch to back this schedule row (see additive-then-deprecate note on
     *  LabSchedule.batch) — when supplied, batchName should mirror the picked Batch's name. */
    Long batchId
) {}
