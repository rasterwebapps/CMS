package com.cms.dto;

/** Null means no institution-wide cap configured for that tier -- matches the existing
 *  "blank/zero = no cap" convention {@code TimetableStaffingService.resolveCapHours} already uses. */
public record FacultyWorkloadRulesResponse(
    Double maxDailyHours,
    Double maxWeeklyHours,
    Double maxContinuousHours
) {}
