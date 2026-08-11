package com.cms.dto;

import com.cms.model.enums.DayOfWeek;

import jakarta.validation.constraints.NotNull;

/** Moves an already-placed skeleton cell to a different day/period, re-running the same
 *  placement (and, for an already-staffed cell, staffing) constraint checks at the target slot. */
public record SkeletonCellMoveRequest(
    @NotNull(message = "Day of week is required")
    DayOfWeek dayOfWeek,

    @NotNull(message = "Period is required")
    Long periodId,

    /** Same rationale as {@link SkeletonCellPlacementRequest#cohortId()} — CourseOffering has no
     *  enforced Cohort FK, so the caller (which already has the cohort selected) passes it. */
    @NotNull(message = "Cohort is required")
    Long cohortId
) {}
