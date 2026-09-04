package com.cms.dto;

/** One candidate in an eligible-faculty picker (offering-level or section-level) — eligibility per
 *  {@code FacultyEligibility.eligibleFaculty} (Speciality match OR the subject's admin-curated
 *  Eligible Faculty list), annotated with real term-demand capacity so the picker can be sorted
 *  most-free-first instead of an unordered name list. {@code currentlyAssigned} marks the faculty
 *  already holding this slot even when they don't otherwise pass eligibility (grandfathered, same
 *  as the dialog's prior client-side filtering) so an existing assignment predating this rule never
 *  silently disappears from the list. {@code capacityTier == "NONE"} means no cap is configured at
 *  any tier for this candidate — {@code remainingHours}/{@code overCapacity} are meaningless in that
 *  case (never flagged over capacity, matching how an unconfigured cap is treated everywhere else in
 *  this codebase) and the picker should render "no cap configured" rather than a 0h figure. */
public record EligibleFacultyCandidateDto(
    Long facultyId,
    String facultyName,
    boolean specialityMatch,
    boolean viaEligibleList,
    boolean currentlyAssigned,
    double currentDemandHours,
    double capacityHours,
    String capacityTier,
    double remainingHours,
    boolean overCapacity
) {}
