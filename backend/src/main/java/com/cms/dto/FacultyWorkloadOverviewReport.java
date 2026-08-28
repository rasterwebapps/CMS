package com.cms.dto;

import java.util.List;

/** Term-wide "required vs assigned" roll-up across every cohort and every active faculty member —
 *  the aggregate counterpart to {@link FacultyWorkloadOverviewRow}'s per-person breakdown.
 *  {@code totalCurriculumRequiredHours} is what the curriculum needs regardless of whether a
 *  faculty has been bound yet (mirrors Skeleton Builder's own per-cohort hours-required total,
 *  summed across every cohort active this term); {@code totalAssignedHours} is what's actually
 *  bound to someone right now ({@code sum(rows[].totalTermDemandHours)}) — the two only match once
 *  {@code unassignedOfferingsCount} is zero. {@code totalFacultyCapacityHours} sums every
 *  configured faculty's term ceiling, independent of how much of it is actually used, so spare
 *  institutional capacity is visible even when every individual row looks "fine". */
public record FacultyWorkloadOverviewReport(
    Long termInstanceId,
    List<FacultyWorkloadOverviewRow> rows,
    double totalCurriculumRequiredHours,
    double totalAssignedHours,
    double totalFacultyCapacityHours,
    int unassignedOfferingsCount
) {}
