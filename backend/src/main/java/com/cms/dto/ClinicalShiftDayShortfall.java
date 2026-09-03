package com.cms.dto;

import java.util.List;

import com.cms.model.enums.DayOfWeek;

/** One cohort/day combination where an active Clinical Shift window leaves too few (or zero) real
 *  on-campus periods free for Theory/Lab that day. See {@link ClinicalShiftPeriodAvailabilityResult}. */
public record ClinicalShiftDayShortfall(
    Long cohortId,
    String cohortDisplayName,
    DayOfWeek dayOfWeek,
    int totalActivePeriods,
    int periodsBlockedByShift,
    int periodsFreeForTheoryLab,
    List<String> affectedShiftLabels
) {}
