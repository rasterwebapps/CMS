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
 *  institutional capacity is visible even when every individual row looks "fine".
 *  {@code recommendedAdditionalFacultyCount} is a whole-pool estimate only — {@code
 *  ceil((totalCurriculumRequiredHours - totalFacultyCapacityHours) / oneFacultyTermCapacity)},
 *  where "one faculty" is the average configured daily capacity across every faculty who actually
 *  has one, times this term's working days. It is deliberately NOT a per-subject/eligibility-pool
 *  solve (a subject whose only eligible faculty are all maxed out can be structurally short even
 *  when this aggregate number is 0) — always 0 when there's no aggregate gap or no faculty has any
 *  configured cap to average. */
public record FacultyWorkloadOverviewReport(
    Long termInstanceId,
    List<FacultyWorkloadOverviewRow> rows,
    double totalCurriculumRequiredHours,
    double totalAssignedHours,
    double totalFacultyCapacityHours,
    int unassignedOfferingsCount,
    int recommendedAdditionalFacultyCount
) {}
