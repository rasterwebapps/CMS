package com.cms.dto;

import java.util.List;

/** Live, single-(faculty, offering) capacity check for Course Offerings — would assigning this
 *  faculty to this offering push their real term-wide load over capacity? Same math as {@link
 *  GlobalCapacityPrecheckResult}, just scoped to one candidate instead of scanning the whole term.
 *  {@code capacityTier} is {@code "NONE"} when the candidate has no cap configured at any tier
 *  (never flagged over capacity in that case, matching how workload caps are treated everywhere
 *  else in this codebase — no configured cap means no check, not an error).
 *
 * <p>{@code suggestedMinDailySessions} exists because the field an admin actually edits (Faculty's
 *  {@code plannedDailySessionsOverride}, via the Faculty Detail/Capacity Planner/Global
 *  Auto-Schedule "Raise Cap" flyout) is a count of sessions, not {@code suggestedMinDailyHours}'
 *  hours — telling someone only "raise to at least 6.0h/day" with no session-count equivalent
 *  left them to convert it themselves against each active Period's real (non-1-hour) duration, and
 *  a plausible-looking round number (entering "6" when periods run 50 minutes) can land short of
 *  the real target instead of clearing it. 0 when not over capacity, same as {@code
 *  suggestedMinDailyHours}. */
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
    int suggestedMinDailySessions,
    List<SpreadLoadSuggestion> spreadLoad
) {}
