package com.cms.dto;

/** Advisory-only suggestion to raise a faculty's daily workload cap to the minimum that would fit
 *  their real total term demand across the term's real working days — never applied automatically,
 *  the admin acts on it via the existing Faculty edit form. */
public record RaiseCapSuggestion(
    Long facultyId,
    double currentDailyCap,
    String currentTier,
    double suggestedMinDailyHours
) {}
