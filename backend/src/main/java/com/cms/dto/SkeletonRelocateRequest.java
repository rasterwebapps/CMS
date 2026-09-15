package com.cms.dto;

import com.cms.model.enums.DayOfWeek;

import jakarta.validation.constraints.NotNull;

/** Move a session (with its whole block) to the same-length window starting at this day/period,
 *  swapping it with whatever already fills that window. */
public record SkeletonRelocateRequest(
    @NotNull(message = "Day of week is required")
    DayOfWeek dayOfWeek,

    @NotNull(message = "Start period is required")
    Long startPeriodId,

    /** Same rationale as {@link SkeletonCellPlacementRequest#cohortId()}. */
    @NotNull(message = "Cohort is required")
    Long cohortId
) {}
