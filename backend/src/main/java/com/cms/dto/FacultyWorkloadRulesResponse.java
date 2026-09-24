package com.cms.dto;

/** Null means no institution-wide cap/floor configured for that tier -- matches the existing
 *  "blank/zero = not configured" convention {@code TimetableStaffingService.resolveCapSessions}
 *  already uses. Sessions (real Period-row count), not hours. */
public record FacultyWorkloadRulesResponse(
    Integer maxDailySessions,
    Integer maxWeeklySessions,
    Integer maxContinuousSessions,
    Integer minWeeklySessions
) {}
