package com.cms.dto;

/** Advisory-only suggestion to move some of an over-capacity faculty's sessions for one offering
 *  to an alternate, same-speciality faculty with spare term capacity — never applied automatically,
 *  the admin reassigns specific sessions/batches via the existing Staffing screen, which already
 *  allows two sessions of one offering to go to different faculty. {@code isOfferingsSecondaryFaculty}
 *  is true when the candidate is that specific offering's own {@code secondaryFacultyId} — checked
 *  and preferred before scanning the wider department pool. */
public record SpreadLoadSuggestion(
    Long alternateFacultyId,
    String alternateFacultyName,
    boolean isOfferingsSecondaryFaculty,
    double alternateSpareCapacityHours,
    Long courseOfferingId,
    String subjectName
) {}
