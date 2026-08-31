package com.cms.dto;

import java.util.List;

/** One Lab or Clinical venue this run genuinely couldn't place enough sessions against — venue
 *  capacity (not faculty, not a scheduling conflict) is the real ceiling: the venue's own weekly
 *  window count is fixed by real clock-time (lunch-break-bounded half-day blocks), and a subject
 *  needing more students through it than {@code currentCapacity} allows per turn gets split into
 *  proportionally more batches, each independently demanding its full weekly block quota — see
 *  {@code TimetableCapacityPlanningService#splitIntoSequentialBatches}'s {@code turns =
 *  ceil(strength / capacity)}. Raising {@code currentCapacity}, or designating a second venue for
 *  the same subject, is the only remedy that actually creates capacity — reordering which cohort
 *  gets scheduled first only decides who is short, never whether anyone is. This is purely
 *  informational: nothing here is auto-applied, since a venue's real capacity is a physical/
 *  supervisory fact about a training site, never a value safe to infer or change on the scheduler's
 *  own judgment. */
public record VenueCapacityGap(
    Long venueId,
    String venueType,
    String venueName,
    Integer currentCapacity,
    double unplacedHours,
    List<String> affectedSubjectNames,
    /** Parallel to {@code affectedSubjectNames} — see {@code VenueOverCapacity#affectedSubjectIds}
     *  for why the frontend needs this (auto-linking a newly designated second venue back to these
     *  exact subjects on save, instead of leaving it invisible to the suggestion engine). */
    List<Long> affectedSubjectIds
) {}
