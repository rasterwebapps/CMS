package com.cms.dto;

import java.util.List;

import com.cms.model.enums.DayOfWeek;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Atomically places every listed member of one elective group at the same day/period. If the
 *  group already has a scheduled slot, dayOfWeek/periodId must match it exactly -- this can add
 *  the group's remaining unplaced members to an existing slot, never move an already-set one. */
public record ElectiveGroupPlacementRequest(
    @NotNull(message = "Elective group is required") Long electiveGroupId,
    @NotNull(message = "Term is required") Long termInstanceId,
    @NotNull(message = "Cohort is required") Long cohortId,
    @NotNull(message = "Day of week is required") DayOfWeek dayOfWeek,
    @NotNull(message = "Period is required") Long periodId,
    @NotEmpty(message = "At least one member placement is required") @Valid List<ElectiveGroupMemberPlacement> members
) {}
