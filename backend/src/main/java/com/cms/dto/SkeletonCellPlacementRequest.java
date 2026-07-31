package com.cms.dto;

import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;

import jakarta.validation.constraints.NotNull;

/** No faculty/room fields on purpose — the skeleton stage (R3 Phase 4) only places
 *  period+type+subject; Phase 5's staffing pass fills in faculty/room afterward. */
public record SkeletonCellPlacementRequest(
    @NotNull(message = "Course offering is required")
    Long courseOfferingId,

    @NotNull(message = "Session type is required")
    ClassSessionType sessionType,

    @NotNull(message = "Day of week is required")
    DayOfWeek dayOfWeek,

    @NotNull(message = "Period is required")
    Long periodId,

    /** Required for LAB/CLINICAL (each batch needs its own placement); optional for THEORY
     *  (R3 Phase 3 section-scoping — null means the whole cohort). */
    Long batchId
) {}
