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
 *  method is eventually removed.
 *
 * <p>{@code capacityCausedGapHours}/{@code recommendedAdditionalFacultyCount} are this run's real,
 *  exact count of Monday-Friday periods the Self-Study/Co-curricular gap-fill pass genuinely
 *  couldn't staff (every eligible faculty already at their capacity cap), converted to hours and
 *  then to a rough headcount — distinct from {@code FacultyWorkloadOverviewReport}'s pre-run
 *  whole-pool estimate, which is raw aggregate hours only and never reflects real day/period
 *  feasibility. Both 0 when this run left nothing genuinely unfillable.
 *
 * <p>{@code venueCapacityGaps} is the LAB/CLINICAL analogue: every Lab or Clinical venue whose own
 *  weekly window capacity — not faculty, not a room/faculty conflict — is the reason this run
 *  couldn't place everything still short against it (see {@link VenueCapacityGap}). Empty when no
 *  venue was the real ceiling this run. */
public record GlobalAutoScheduleResult(
    int totalPlaced,
    int totalStaffed,
    List<CohortPlacementSummary> cohortSummaries,
    List<AutoPlaceUnplacedItem> electiveUnplaced,
    int staleDraftsCleared,
    double capacityCausedGapHours,
    int recommendedAdditionalFacultyCount,
    List<VenueCapacityGap> venueCapacityGaps
) {}
