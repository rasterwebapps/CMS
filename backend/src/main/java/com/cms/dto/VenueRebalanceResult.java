package com.cms.dto;

import java.util.List;

/** What {@code TimetableCapacityPlanningService#applyRebalance} actually did — {@code
 *  sessionsCleared} is the total DRAFT {@code ClassSchedule} rows deactivated across every moved
 *  batch; the admin needs to re-run Run Automation afterward to re-place them at their new venue,
 *  this method never does that itself. */
public record VenueRebalanceResult(
    int batchesMoved,
    int sessionsCleared,
    List<VenueRebalanceMove> moved
) {}
