package com.cms.dto;

import java.util.List;

/** One Lab or Clinical venue whose total real weekly demand is at or near (but not over)
 *  {@code weeklyAvailablePeriods} — distinct from {@link VenueOverCapacity}, which only fires
 *  once demand actually exceeds the venue's weekly window. A venue at ~100% weekly utilization
 *  has effectively zero slack: the raw period total "fits" on paper, but the real backtracking
 *  placement still has to fit every batch's exact block into that window without fragmentation or
 *  clashing with faculty availability — nothing here guarantees that real placement succeeds,
 *  only that the raw period totals don't already rule it out. Surfaced so an admin sees the real
 *  risk before running, rather than discovering it only as an unplaced session afterward. */
public record VenueTightCapacity(
    Long venueId,
    String venueType,
    String venueName,
    Integer capacity,
    int weeklyAvailablePeriods,
    int weeklyDemandPeriods,
    /** 0-100; always >= the tight-capacity threshold and < 100 + epsilon (at/over 100 would have
     *  already been reported as {@link VenueOverCapacity} instead). */
    double utilizationPercent,
    List<String> affectedSubjectNames,
    /** Parallel to {@code affectedSubjectNames} — see {@code VenueOverCapacity#affectedSubjectIds}
     *  for why the frontend needs this to auto-link a newly created second venue. */
    List<Long> affectedSubjectIds
) {}
