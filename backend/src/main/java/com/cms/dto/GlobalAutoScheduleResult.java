package com.cms.dto;

import java.util.List;

/** Result of {@code TimetableGlobalAutoScheduleService.runGlobalAutoSchedule} — best-effort: always
 *  returned once the capacity precheck passes, reporting everything actually placed/staffed plus
 *  everything it couldn't, per cohort ({@code cohortSummaries[].unplaced()}) and, for elective
 *  groups (not attributable to a single cohort), in {@code electiveUnplaced}.
 *
 * <p>{@code staleDraftsCleared} is how many existing DRAFT sessions {@code
 *  purgeDraftCellsForRebuild} cleared before this run re-placed anything. Every run rebuilds the
 *  whole DRAFT grid for the cohorts in scope rather than adding on top of it (see that method's
 *  javadoc for the fragmentation incident that forced this), so on a re-run this is normally the
 *  cohort's entire previous draft, not an anomaly — surfaced here so the replacement is never a
 *  silent surprise.
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
 *  venue was the real ceiling this run.
 *
 * <p>{@code skippedPublishedCohorts} lists every cohort this "All Cohorts" run deliberately left
 *  untouched because this term's timetable is already approved/{@code PUBLISHED} on Draft Review —
 *  never populated for a single-cohort run (that case is a hard block at the API boundary instead,
 *  see {@code TimetableGlobalAutoScheduleService#runGlobalAutoSchedule}). Always empty while the
 *  term is still in DRAFT. */
public record GlobalAutoScheduleResult(
    int totalPlaced,
    int totalStaffed,
    List<CohortPlacementSummary> cohortSummaries,
    List<AutoPlaceUnplacedItem> electiveUnplaced,
    int staleDraftsCleared,
    double capacityCausedGapHours,
    int recommendedAdditionalFacultyCount,
    List<VenueCapacityGap> venueCapacityGaps,
    List<SkippedPublishedCohort> skippedPublishedCohorts
) {}
