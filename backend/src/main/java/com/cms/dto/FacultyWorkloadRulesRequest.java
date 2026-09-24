package com.cms.dto;

import jakarta.validation.constraints.Min;

/** Null (or omitted) means no institution-wide cap/floor for that tier. A negative value is
 *  rejected outright; 0 is accepted but stored as "not configured" (same convention as the rest of
 *  this system). Sessions (real Period-row count), not hours -- see FacultyWorkloadRulesService's
 *  own doc comment for why. */
public record FacultyWorkloadRulesRequest(
    @Min(value = 0, message = "Max daily sessions cannot be negative") Integer maxDailySessions,
    @Min(value = 0, message = "Max weekly sessions cannot be negative") Integer maxWeeklySessions,
    @Min(value = 0, message = "Max continuous sessions cannot be negative") Integer maxContinuousSessions,
    @Min(value = 0, message = "Minimum weekly sessions cannot be negative") Integer minWeeklySessions
) {}
