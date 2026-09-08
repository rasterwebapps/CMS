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
 *  term is still in DRAFT.
 *
 * <p>{@code rotationGroupsCreated} is how many cross-offering LAB pairings this run's Phase B
 *  ({@code TimetableGlobalAutoScheduleService#attemptCrossOfferingPairing}) turned into a real
 *  {@code RotationGroup} — 0 on a run with no eligible offering pair, never an error. {@code
 *  pairingSkipReasons} names every candidate pair Phase B considered but couldn't actually place
 *  (no shared free day/period found for both labs+all batches+both faculty) — distinct from a pair
 *  simply never being eligible in the first place (mismatched batch counts, different block sizes,
 *  etc.), which is silent by design since that's the normal, expected shape for most offerings.
 *
 * <p>{@code facultySubstitutionTips} lists every subject where this run had to place one or more
 *  sessions with a different, already-eligible faculty member instead of the offering's own bound
 *  faculty, because that bound faculty was unavailable at every remaining slot (see {@code
 *  TimetableGlobalAutoScheduleService#recordFacultySubstitutionIfAny}). The substitute sessions are
 *  already placed and staffed by the time this is reported — purely an actionable tip ("consider
 *  reassigning this offering") for an admin, never a pending action of its own. Empty on the common
 *  run where every row's own bound faculty covered everything it needed to. */
public record GlobalAutoScheduleResult(
    int totalPlaced,
    int totalStaffed,
    List<CohortPlacementSummary> cohortSummaries,
    List<AutoPlaceUnplacedItem> electiveUnplaced,
    int staleDraftsCleared,
    double capacityCausedGapHours,
    int recommendedAdditionalFacultyCount,
    List<VenueCapacityGap> venueCapacityGaps,
    List<SkippedPublishedCohort> skippedPublishedCohorts,
    int rotationGroupsCreated,
    List<String> pairingSkipReasons,
    List<FacultySubstitutionTip> facultySubstitutionTips
) {}
