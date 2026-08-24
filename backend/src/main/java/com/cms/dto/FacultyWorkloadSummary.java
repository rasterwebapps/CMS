package com.cms.dto;

/** Lightweight per-faculty workload summary — backs a list-row badge (Faculty List), not a detail
 *  view. See {@link FacultyWorkloadDetail} for the full per-assignment breakdown. */
public record FacultyWorkloadSummary(
    Long facultyId,
    double totalDemandHours,
    double termCapacityHours,
    boolean overCapacity,
    double shortfallHours
) {}
