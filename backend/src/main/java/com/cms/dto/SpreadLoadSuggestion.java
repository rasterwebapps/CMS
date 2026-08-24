package com.cms.dto;

/** Suggestion to move some of an over-capacity faculty's sessions for one offering to an alternate,
 *  same-speciality faculty with spare term capacity. {@code isOfferingsSecondaryFaculty} is true
 *  when the candidate is that specific offering's own {@code secondaryFacultyId} — checked and
 *  preferred before scanning the wider department pool. {@code cohortSectionId}/{@code batchId}
 *  (at most one non-null, mirroring {@link OverageContributor}) identify exactly which section or
 *  batch this suggestion's contribution came from — an "Assign" action can reassign that specific
 *  section's Course Offering Section Faculty override or that batch's coordinator to actually move
 *  the load, rather than this staying purely advisory text; both null means the offering's
 *  whole-cohort primary, which has no section/batch to reassign at (still resolvable via the
 *  existing Staffing screen instead). */
public record SpreadLoadSuggestion(
    Long alternateFacultyId,
    String alternateFacultyName,
    boolean isOfferingsSecondaryFaculty,
    double alternateSpareCapacityHours,
    Long courseOfferingId,
    String subjectName,
    Long cohortSectionId,
    Long batchId
) {}
