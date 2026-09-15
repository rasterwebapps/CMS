package com.cms.dto;

import java.util.List;

/** {@code unplaced} lists every shortfall unit this cohort couldn't place/staff this run (best-
 *  effort — see {@code TimetableGlobalAutoScheduleService#runGlobalAutoSchedule}), reconciled
 *  against the finished grid in real term hours. {@code usedSaturday} flags whether any of
 *  {@code placedCount} landed on Saturday — a regular working day whenever the term has chosen
 *  working Saturdays. {@code infoNotes} are neutral, expected outcomes (e.g. Library shrinking to
 *  fit a full week) — never warnings. */
public record CohortPlacementSummary(
    Long cohortId,
    String cohortName,
    int placedCount,
    int staffedCount,
    List<AutoPlaceUnplacedItem> unplaced,
    boolean usedSaturday,
    List<String> infoNotes
) {}
