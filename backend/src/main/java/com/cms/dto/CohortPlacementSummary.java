package com.cms.dto;

import java.util.List;

/** {@code unplaced} lists every shortfall unit this cohort couldn't place/staff this run (best-
 *  effort — see {@code TimetableGlobalAutoScheduleService#runGlobalAutoSchedule}); {@code
 *  usedSaturday} flags whether any of {@code placedCount} landed on Saturday, since Monday-Friday
 *  is the automation's preferred window and a Saturday placement is a fact worth surfacing, not
 *  silently absorbing. */
public record CohortPlacementSummary(
    Long cohortId,
    String cohortName,
    int placedCount,
    int staffedCount,
    List<AutoPlaceUnplacedItem> unplaced,
    boolean usedSaturday
) {}
