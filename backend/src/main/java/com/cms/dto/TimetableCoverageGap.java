package com.cms.dto;

import com.cms.model.enums.ClassSessionType;

/** One (cohort, session type) combination whose curriculum-required hours are not fully placed as
 *  real {@link com.cms.model.ClassSchedule} rows for the term — surfaced by {@code
 *  TimetableCoverageService#findGaps} to gate Draft Review's Approve action. {@code sessionType} is
 *  always THEORY/LAB/CLINICAL — Library/Sports have no curriculum-hours budget (see {@code
 *  TimetableCoverageCalculator}), matching Skeleton Builder's own hours-summary cards. */
public record TimetableCoverageGap(
    Long cohortId,
    String cohortName,
    ClassSessionType sessionType,
    double totalHours,
    double assignedHours,
    double unassignedHours
) {}
