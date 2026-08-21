package com.cms.dto;

import java.util.List;

/** Live, single-(faculty, offering) capacity check for Course Offerings — would assigning this
 *  faculty to this offering push their real term-wide load over capacity? Same math as {@link
 *  GlobalCapacityPrecheckResult}, just scoped to one candidate instead of scanning the whole term.
 *  {@code capacityTier} is {@code "NONE"} when the candidate has no cap configured at any tier
 *  (never flagged over capacity in that case, matching how workload caps are treated everywhere
 *  else in this codebase — no configured cap means no check, not an error). */
public record FacultyCapacityCheckResult(
    boolean overCapacity,
    double currentDemandHours,
    double offeringHours,
    double projectedTotalHours,
    double capacityHours,
    double dailyCap,
    String capacityTier,
    int workingDaysInTerm,
    double suggestedMinDailyHours,
    List<SpreadLoadSuggestion> spreadLoad
) {}
