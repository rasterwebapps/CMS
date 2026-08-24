package com.cms.dto;

import java.util.List;

/** Consolidated prerequisite report for {@code TimetableGlobalAutoScheduleService.runGlobalAutoSchedule}
 *  — checked once, up front, so every shortfall (missing faculty, over-capacity faculty) is
 *  reported together as actionable links before the automation is ever run, rather than discovered
 *  one gate at a time. Room-commit status is deliberately not part of this DTO — it's checked
 *  client-side against Capacity Planner's own existing endpoints (a different domain), not
 *  duplicated here. {@code ready()} is true only when both lists are empty. */
public record GlobalAutoSchedulePrerequisites(
    List<UnassignedOfferingSummary> offeringsWithoutFaculty,
    GlobalCapacityPrecheckResult capacityPrecheck
) {
    public boolean ready() {
        return offeringsWithoutFaculty.isEmpty() && capacityPrecheck.overCapacityFaculty().isEmpty();
    }
}
