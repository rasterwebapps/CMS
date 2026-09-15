package com.cms.dto;

import com.cms.model.enums.DayOfWeek;

import jakarta.validation.constraints.NotNull;

/** Move a Clinical Shift group's duty to another day for the cohort being built. */
public record DutyDayMoveRequest(
    @NotNull(message = "Day of week is required")
    DayOfWeek dayOfWeek,

    @NotNull(message = "Cohort is required")
    Long cohortId
) {}
