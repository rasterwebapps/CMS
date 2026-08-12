package com.cms.dto;

import jakarta.validation.constraints.DecimalMin;

/** Null (or omitted) means no institution-wide cap for that tier. A negative value is rejected
 *  outright; 0 is accepted but stored as "no cap" (same convention as the rest of this system). */
public record FacultyWorkloadRulesRequest(
    @DecimalMin(value = "0.0", message = "Max daily hours cannot be negative") Double maxDailyHours,
    @DecimalMin(value = "0.0", message = "Max weekly hours cannot be negative") Double maxWeeklyHours,
    @DecimalMin(value = "0.0", message = "Max continuous hours cannot be negative") Double maxContinuousHours
) {}
