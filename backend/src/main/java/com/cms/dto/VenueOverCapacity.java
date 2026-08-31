package com.cms.dto;

import java.util.List;

/** One Lab or Clinical venue whose total real weekly demand — summed across every batch (real
 *  committed {@code Batch} rows plus not-yet-committed cohorts' suggested batches) sharing it
 *  this term — exceeds {@code weeklyAvailablePeriods}, its real weekly (day, period) window
 *  (Monday-Friday always, Saturday only when the term has a working-Saturday pattern, minus any
 *  term-wide recurring blocked period). This is a necessary-condition check, not a true day/period
 *  collision simulation: {@code shortfallPeriods > 0} proves the demand can NEVER fit regardless
 *  of arrangement, since it's a raw weekly-period total, not a claim that a specific conflict-free
 *  schedule doesn't exist. See {@code TimetableCapacityPlanningService#computeLabClinicalVenueCapacity}.
 *
 * <p>{@code affectedSubjectIds} (parallel to {@code affectedSubjectNames}, same order/dedup) lets
 *  the frontend's "Add a second venue" remedy pass these exact subjects straight through to the
 *  new venue's create form, which then calls {@code SubjectService#addEligibleVenue} on save so
 *  the newly created venue is immediately eligible for the same subjects already stuck on this
 *  one -- without this, a freshly created venue is otherwise invisible to the suggestion engine
 *  (DESIGNATED-ONLY selection, see {@code TimetableCapacityPlanningService#eligibleAndActive})
 *  until an admin separately visits each Subject's edit form. */
public record VenueOverCapacity(
    Long venueId,
    String venueType,
    String venueName,
    Integer capacity,
    int weeklyAvailablePeriods,
    int weeklyDemandPeriods,
    int shortfallPeriods,
    List<String> affectedSubjectNames,
    List<Long> affectedSubjectIds
) {}
