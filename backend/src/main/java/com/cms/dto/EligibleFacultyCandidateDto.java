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
    boolean overCapacity,

    /** Why assigning this candidate to one SPECIFIC already-placed session would be refused, or
     *  null if it wouldn't. Populated only when the caller names a session (the {@code
     *  classScheduleId} parameter on the section/cohort endpoints); always null otherwise, which is
     *  how every caller that picks a faculty for a whole offering rather than one slot behaves.
     *
     *  <p>This exists because the other fields on this record measure a completely different thing
     *  from what the save actually enforces. {@code remainingHours}/{@code overCapacity} are TERM
     *  totals — useful for planning a whole offering — while staffing a particular session is
     *  refused by the daily, weekly and continuous caps for that session's own day and time. The
     *  two regularly disagree: a faculty member can read "651h free this term" and still be
     *  refused a 09:00 Monday slot because a 6h clinical duty already fills that day. A picker
     *  showing only the term figure therefore recommends people the validator then rejects, which
     *  is exactly what happened before this field existed.
     *
     *  <p>Computed with the same {@code TimetableStaffingService#checkWithinWorkloadCaps} the save
     *  path runs, so the two can't drift. First violation only — the picker needs a reason to show,
     *  not an exhaustive list. */
    String slotBlockedReason
) {}
