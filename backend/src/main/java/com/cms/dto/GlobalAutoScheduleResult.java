package com.cms.dto;

import java.util.List;

/** Result of {@code TimetableGlobalAutoScheduleService.runGlobalAutoSchedule} — best-effort: always
 *  returned once the capacity precheck passes, reporting everything actually placed/staffed plus
 *  everything it couldn't, per cohort ({@code cohortSummaries[].unplaced()}) and, for elective
 *  groups (not attributable to a single cohort), in {@code electiveUnplaced}.
 *
 * <p>{@code staleDraftsCleared} is the count {@code purgeStaleOverBudgetDrafts} removed as a
 *  pre-flight step before this run placed anything — a TEMPORARY safety net (see that method's
 *  own javadoc), surfaced here so a nonzero count is never a silent surprise. Always 0 once that
 *  method is eventually removed. */
public record GlobalAutoScheduleResult(
    int totalPlaced,
    int totalStaffed,
    List<CohortPlacementSummary> cohortSummaries,
    List<AutoPlaceUnplacedItem> electiveUnplaced,
    int staleDraftsCleared
) {}
