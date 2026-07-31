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

    /** Required when sessionType = LAB or CLINICAL. */
    String batchName,

    /** Optional real Batch to back a LAB/CLINICAL row's roster group (see additive-then-deprecate
     *  note on ClassSchedule.batch) — when supplied, batchName should mirror the picked Batch's
     *  name. Also usable on a THEORY row (R3 Phase 3) to scope that subject's Theory schedule to
     *  one section instead of the whole cohort — left null there means "whole cohort", the only
     *  behavior that existed before Phase 3. */
    Long batchId,

    @NotNull(message = "Day of week is required")
    DayOfWeek dayOfWeek,

    @NotNull(message = "Term instance ID is required")
    Long termInstanceId,

    Boolean isActive,

    /** Required when sessionType = THEORY. */
    Long classroomId,

    @NotNull(message = "Period is required")
    Long periodId,

    /** Required when sessionType = CLINICAL. */
    Long clinicalVenueId,

    /** Optional context linking this row back to a CourseOffering — required for THEORY rows
     *  to resolve audience via CourseRegistration; optional for LAB/CLINICAL rows. */
    Long courseOfferingId
) {}
