package com.cms.dto;

/** Null clears the daily-hours-override, same semantics as leaving the field blank on the full
 *  Faculty edit form. */
public record DailyCapUpdateRequest(Integer plannedDailyHoursOverride) {}
