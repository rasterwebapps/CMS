package com.cms.dto;

/** One non-elective offering with a shortfall but no faculty bound yet — reported as a prerequisite
 *  gap by {@code TimetableGlobalAutoScheduleService#checkPrerequisites} so the admin can fix it
 *  (via Assign Faculty) before the automation is even offered, rather than discovering it as an
 *  opaque unplaced item after a run. */
public record UnassignedOfferingSummary(
    Long courseOfferingId,
    String subjectName,
    Long cohortId,
    String cohortName
) {}
