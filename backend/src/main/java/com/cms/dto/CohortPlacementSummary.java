package com.cms.dto;

import java.util.List;

/** {@code unplaced} lists every shortfall unit this cohort couldn't place/staff this run (best-
 *  effort — see {@code TimetableGlobalAutoScheduleService#runGlobalAutoSchedule}); {@code
 *  usedSaturday} flags whether any of {@code placedCount} landed on Saturday. Saturday is no
 *  longer a deprioritized fallback — when the term has working-Saturday weeks configured, it
 *  competes for content on the same least-loaded-day-first footing as any weekday (see
 *  {@code TimetableGlobalAutoScheduleService#tryPlaceAndStaff}) — so this flag is purely
 *  informational now, not a signal that something unusual happened. */
public record CohortPlacementSummary(
    Long cohortId,
    String cohortName,
    int placedCount,
    int staffedCount,
    List<AutoPlaceUnplacedItem> unplaced,
    boolean usedSaturday
) {}
