package com.cms.dto;

import java.util.List;

/** One faculty whose total real term-hour demand — summed across every {@code CourseOffering}
 *  they're bound to ({@code facultyId}) across every cohort active in the term — exceeds their
 *  effective term capacity. {@code dailyCapacityTier} names which of the 3-tier chain
 *  (per-faculty override / designation default / global {@code SystemConfiguration}) is actually
 *  binding, so the report can point at the right knob to turn. */
public record FacultyOverCapacity(
    Long facultyId,
    String facultyName,
    double effectiveDailyCapacityHours,
    String dailyCapacityTier,
    int workingDaysInTerm,
    double termCapacityHours,
    double totalTermDemandHours,
    double shortfallHours,
    double suggestedMinDailyHours,
    List<OverageContributor> topContributors,
    RaiseCapSuggestion raiseCap,
    List<SpreadLoadSuggestion> spreadLoad
) {}
