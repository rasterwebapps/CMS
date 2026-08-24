package com.cms.dto;

import java.util.List;

/** One faculty's real, full term workload breakdown — every offering/section/batch contributing
 *  to their demand (unlimited, unlike {@code FacultyOverCapacity.topContributors}' top-2 slice),
 *  plus their resolved capacity. Backs the Faculty Detail "Courses" tab. */
public record FacultyWorkloadDetail(
    Long facultyId,
    String facultyName,
    Long termInstanceId,
    int workingDaysInTerm,
    double effectiveDailyCapacityHours,
    String dailyCapacityTier,
    double termCapacityHours,
    double totalDemandHours,
    boolean overCapacity,
    double shortfallHours,
    List<OverageContributor> assignments
) {}
