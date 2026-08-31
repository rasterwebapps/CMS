package com.cms.dto;

import java.util.List;

/** Consolidated prerequisite report for {@code TimetableGlobalAutoScheduleService.runGlobalAutoSchedule}
 *  — checked once, up front, so every shortfall (missing faculty, over-capacity faculty,
 *  over-capacity Lab/Clinical venue) is reported together as actionable links before the
 *  automation is ever run, rather than discovered one gate at a time. General room-commit status
 *  is still deliberately not part of this DTO — it's checked client-side against Capacity
 *  Planner's own existing endpoints (a different domain), not duplicated here. {@code
 *  labClinicalVenueCapacity} is the one deliberate exception to that boundary: unlike a Theory
 *  classroom (committed once, per cohort, for the whole term), a shared Lab/Clinical venue's real
 *  weekly feasibility is exactly what Run Automation itself can fail on, so it belongs in this
 *  report the same way faculty capacity does. {@code ready()} is true only when every list is
 *  empty. */
public record GlobalAutoSchedulePrerequisites(
    List<UnassignedOfferingSummary> offeringsWithoutFaculty,
    GlobalCapacityPrecheckResult capacityPrecheck,
    LabClinicalVenueCapacityResult labClinicalVenueCapacity
) {
    public boolean ready() {
        return offeringsWithoutFaculty.isEmpty() && capacityPrecheck.overCapacityFaculty().isEmpty()
            && labClinicalVenueCapacity.overCapacityVenues().isEmpty();
    }
}
