package com.cms.dto;

import java.util.List;

/** Result of {@code TimetableGlobalAutoScheduleService.runGlobalAutoSchedule} — best-effort: always
 *  returned once the capacity precheck passes, reporting everything actually placed/staffed plus
 *  everything it couldn't, per cohort ({@code cohortSummaries[].unplaced()}) and, for elective
 *  groups (not attributable to a single cohort), in {@code electiveUnplaced}. */
public record GlobalAutoScheduleResult(
    int totalPlaced,
    int totalStaffed,
    List<CohortPlacementSummary> cohortSummaries,
    List<AutoPlaceUnplacedItem> electiveUnplaced
) {}
