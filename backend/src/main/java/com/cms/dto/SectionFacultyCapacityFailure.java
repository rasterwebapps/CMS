package com.cms.dto;

/** One over-capacity substitute rejected by {@link
 *  com.cms.service.CourseOfferingSectionFacultyService#confirmSubstitutions} -- {@code facultyId}
 *  is who would have gone over capacity (the substitute being confirmed), not the original faculty
 *  they were replacing. Lets the frontend attribute this failure back to the exact Global
 *  Auto-Schedule substitution tip it came from and offer a fix (raise their cap, or reassign to
 *  someone else) right on that tip's own card. {@code alternateFacultyId} is the same person named
 *  in {@code message}'s "...or assign X instead" clause (the check's own top spread-load
 *  suggestion) -- null when the check found no alternate with spare capacity to suggest; lets the
 *  frontend pre-select them in the reassign picker instead of the admin re-finding that same name.
 *  {@code suggestedMinDailySessions} is the same number named in {@code message}'s "...raise their
 *  cap to at least N session(s)/day..." clause -- the actual value the Raise Cap flyout's field
 *  accepts (a session count, not hours) -- so the frontend can pre-fill that flyout with a number
 *  that genuinely clears this block instead of leaving the admin to convert hours to sessions by
 *  hand against each Period's real (non-1-hour) duration. */
public record SectionFacultyCapacityFailure(
    Long courseOfferingId,
    Long facultyId,
    String message,
    Long alternateFacultyId,
    int suggestedMinDailySessions
) {
}
