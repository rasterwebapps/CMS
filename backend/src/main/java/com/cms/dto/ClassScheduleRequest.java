package com.cms.dto;

import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;

import jakarta.validation.constraints.NotNull;

public record ClassScheduleRequest(
    @NotNull(message = "Session type is required")
    ClassSessionType sessionType,

    /** Required when sessionType = LAB. */
    Long labId,

    @NotNull(message = "Subject ID is required")
    Long subjectId,

    @NotNull(message = "Faculty ID is required")
    Long facultyId,

    /** Required when sessionType = LAB. */
    Long labSlotId,

    /** Required when sessionType = LAB. */
    String batchName,

    /** Optional real Batch to back a LAB row (see additive-then-deprecate note on
     *  ClassSchedule.batch) — when supplied, batchName should mirror the picked Batch's name. */
    Long batchId,

    @NotNull(message = "Day of week is required")
    DayOfWeek dayOfWeek,

    @NotNull(message = "Term instance ID is required")
    Long termInstanceId,

    Boolean isActive,

    /** Required when sessionType = THEORY. */
    Long classroomId,

    /** Required when sessionType = THEORY. */
    Long periodId,

    /** Optional context linking this row back to a CourseOffering — required for THEORY rows
     *  to resolve audience via CourseRegistration; optional for LAB rows. */
    Long courseOfferingId
) {}
