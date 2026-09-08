package com.cms.dto;

import java.util.List;

/** One {@link FacultySubstitutionTip} the admin ticked to confirm as the real, permanent Theory
 *  assignment — see {@code CourseOfferingSectionFacultyService#confirmSubstitutions}. Carries no
 *  row version: the service re-resolves each row named in {@code affectedSections} (copied verbatim
 *  from the tip that produced this item) at request time and reassigns it to {@code
 *  substituteFacultyId} using its own live version, rather than trusting a version captured back
 *  when the run itself finished. {@code affectedSections} is what scopes this item to exactly the
 *  row(s) this tip's own fallback covered — never every row in the offering still on {@code
 *  originalFacultyId}, which could also reassign a sibling section a *different*, separately-ticked
 *  tip already claimed. */
public record ConfirmFacultySubstitutionItem(
    Long courseOfferingId,
    Long originalFacultyId,
    Long substituteFacultyId,
    List<SubstitutionAffectedSection> affectedSections
) {}
