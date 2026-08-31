export type SkeletonSessionType = 'THEORY' | 'LAB' | 'CLINICAL';
export type SkeletonCellStatus = 'DRAFT' | 'PUBLISHED';

export interface SkeletonSubjectBudget {
  sessionType: SkeletonSessionType;
  batchId: number | null;
  batchName: string | null;
  /** Non-null for a THEORY row once the cohort has a committed Cohort Room Allocation with one or
   *  more active sections (one budget row per section instead of a single whole-cohort row), or
   *  for a LAB/CLINICAL row whose batch itself belongs to a section. Kept separate from
   *  batchId/batchName since they're semantically distinct occupants. */
  cohortSectionId: number | null;
  cohortSectionLabel: string | null;
  totalHours: number;
  weeksInTerm: number;
  requiredSessionsPerWeek: number;
  placedSessionsPerWeek: number;
}

export interface SkeletonSubject {
  courseOfferingId: number;
  subjectName: string;
  subjectCode: string;
  budgets: SkeletonSubjectBudget[];
  /** Non-null only for a grouped elective subject — every subject sharing the same group id must
   *  be placed in the same day/period this term (enforced server-side on placement). */
  electiveGroupId: number | null;
  electiveGroupName: string | null;
}

export interface SkeletonCell {
  id: number;
  sessionType: SkeletonSessionType;
  dayOfWeek: string;
  periodId: number;
  slotName: string;
  startTime: string;
  endTime: string;
  batchId: number | null;
  batchName: string | null;
  /** Which CohortSection this cell belongs to — set directly for a sectioned THEORY cell, or
   *  (LAB/CLINICAL) derived server-side from the placed batch's own CohortSection. Null only for a
   *  cell with no section at all (unsectioned cohort, or a legacy batch predating sectioning). */
  cohortSectionId: number | null;
  cohortSectionLabel: string | null;
  isStaffed: boolean;
  status: SkeletonCellStatus;
  /** Non-null only for a cell that's part of a Rotation Group — batchId/batchName are null on
   *  those (there's no single fixed occupant); rotatingBatchNames lists who alternates through it. */
  rotationGroupLabel: string | null;
  rotatingBatchNames: string[];
  courseOfferingId: number;
  subjectName: string;
  subjectCode: string;
  electiveGroupId: number | null;
  electiveGroupName: string | null;
  /** Non-null only for a cell that's part of a multi-period session (periodSpan) — every sibling
   *  cell sharing this id was placed/staffed/removed together as one atomic unit. */
  sessionGroupId: string | null;
}

export interface SkeletonBatchOption {
  id: number;
  courseOfferingId: number;
  name: string;
  capacity: number;
  enrolledCount: number;
}

/** One committed Theory room/section a THEORY placement can target — mirrors the backend's
 *  CohortSectionResponse (reused directly there rather than a duplicated shape). */
export interface SkeletonSectionOption {
  id: number;
  sectionLabel: string;
  classroomId: number;
  classroomName: string;
  classroomCapacity: number;
  plannedSize: number;
  isActive: boolean;
}

/** Cohort-wide since R3.1 — one response covers every non-elective subject a cohort has in a
 *  term, merging their cells/batches so cross-subject placement conflicts are visible in a
 *  single grid instead of hidden behind a per-subject filter. `sections` lists the cohort's
 *  active Cohort Room Allocation sections for this term (empty if none committed).
 *  `weeksInTerm`/`workingSaturdayCount` are term-wide constants (not per-subject) used to compute
 *  an honest scheduled-hours total from `cells`: a Mon-Fri cell recurs `weeksInTerm` times, a
 *  Saturday-placed one only recurs `workingSaturdayCount` times (0 if no working-Saturday pattern
 *  is configured for the term — Saturday is opt-in, off by default). */
export interface SkeletonBuilderResponse {
  cohortId: number;
  cohortName: string;
  termInstanceLabel: string;
  subjects: SkeletonSubject[];
  cells: SkeletonCell[];
  batches: SkeletonBatchOption[];
  sections: SkeletonSectionOption[];
  weeksInTerm: number;
  workingSaturdayCount: number;
}

export interface SkeletonCellPlacementRequest {
  courseOfferingId: number;
  sessionType: SkeletonSessionType;
  dayOfWeek: string;
  periodId: number;
  batchId: number | null;
  cohortId: number;
  /** THEORY only — required whenever the cohort has one or more active sections; null/ignored
   *  for LAB/CLINICAL (their section scope comes from the chosen batch instead). */
  cohortSectionId: number | null;
  /** Additional periods (beyond periodId) this one session also occupies, e.g. a 2-period lab —
   *  must be immediately consecutive with periodId. Null/empty means an ordinary single-period
   *  session (the default). */
  spanPeriodIds: number[] | null;
}

export interface SkeletonPlacementCandidate {
  dayOfWeek: string;
  periodId: number;
}

/** One grid slot's live legality for dragging a specific already-placed cell there — powers the
 *  drag-highlight preview. `reason` is a human-readable violation message when `valid` is false
 *  (matching the backend's first-violation-wins order), null when valid. */
export interface SkeletonSlotPreview {
  dayOfWeek: string;
  periodId: number;
  valid: boolean;
  reason: string | null;
}

export interface SkeletonCellMoveRequest {
  dayOfWeek: string;
  periodId: number;
  cohortId: number;
}

/** Atomically exchanges two already-placed DRAFT cells' day/period — fired instead of a plain
 *  move when a drag lands on a slot that's already occupied by exactly one other cell. */
export interface SkeletonCellSwapRequest {
  targetCellId: number;
  cohortId: number;
}

export interface AutoPlaceUnplacedItem {
  subjectName: string;
  sessionType: SkeletonSessionType;
  occupantLabel: string | null;
  reason: string;
  /** Null only for a whole-elective-group failure (no single offering to point at) — used to
   *  deep-link a Special Class request pre-filled with the right subject. */
  courseOfferingId: number | null;
}


export interface ElectiveGroupMemberPlacement {
  courseOfferingId: number;
  sessionType: SkeletonSessionType;
  batchId: number | null;
  cohortSectionId: number | null;
}

export interface ElectiveGroupPlacementRequest {
  electiveGroupId: number;
  termInstanceId: number;
  cohortId: number;
  dayOfWeek: string;
  periodId: number;
  members: ElectiveGroupMemberPlacement[];
}

export interface ElectiveGroupScheduleResponse {
  scheduled: boolean;
  dayOfWeek: string | null;
  periodName: string | null;
  startTime: string | null;
  endTime: string | null;
}

// ── Global multi-cohort auto-scheduler ─────────────────────────────────────

export interface OverageContributor {
  courseOfferingId: number;
  subjectName: string;
  cohortId: number;
  cohortName: string;
  termHoursContributed: number;
  /** At most one of these is non-null — which section (Theory) or batch (Lab/Clinical) this
   *  contribution came from, so a spread-load suggestion can be turned into a real reassignment
   *  (see {@link SpreadLoadSuggestion}) instead of only advisory text. Both null means the
   *  offering's whole-cohort primary (no active sections/batches to split across). */
  cohortSectionId: number | null;
  batchId: number | null;
  /** Display name matching cohortSectionId/batchId, so two rows for the same subject+cohort
   *  render distinguishably instead of looking like unexplained duplicates. */
  cohortSectionLabel: string | null;
  batchName: string | null;
  /** 'THEORY' | 'LAB' | 'CLINICAL' | 'LAB_CLINICAL' (legacy untyped batch or an unsectioned/
   *  unbatched offering where lab+clinical can't be split further); null only for the synthetic
   *  single-offering contributor used by the live Course Offerings capacity check. */
  sessionType: string | null;
}

export interface RaiseCapSuggestion {
  facultyId: number;
  currentDailyCap: number;
  currentTier: string;
  suggestedMinDailyHours: number;
}

export interface SpreadLoadSuggestion {
  alternateFacultyId: number;
  alternateFacultyName: string;
  alternateSpareCapacityHours: number;
  courseOfferingId: number;
  subjectName: string;
  /** At most one non-null — which section/batch this suggestion can actually be applied to via
   *  "Assign as Section/Batch Faculty". Both null means no direct reassignment target exists
   *  (whole-cohort primary) — resolve via the Staffing screen instead. */
  cohortSectionId: number | null;
  batchId: number | null;
}

export interface FacultyOverCapacity {
  facultyId: number;
  facultyName: string;
  effectiveDailyCapacityHours: number;
  dailyCapacityTier: string;
  workingDaysInTerm: number;
  termCapacityHours: number;
  totalTermDemandHours: number;
  shortfallHours: number;
  suggestedMinDailyHours: number;
  topContributors: OverageContributor[];
  raiseCap: RaiseCapSuggestion;
  spreadLoad: SpreadLoadSuggestion[];
}

/** Not over capacity (a run may proceed once acknowledged) but at/near 100% utilization — real
 *  day/period packing isn't guaranteed to succeed even though the aggregate hours "fit". */
export interface FacultyTightCapacity {
  facultyId: number;
  facultyName: string;
  effectiveDailyCapacityHours: number;
  dailyCapacityTier: string;
  workingDaysInTerm: number;
  termCapacityHours: number;
  totalTermDemandHours: number;
  utilizationPercent: number;
  topContributors: OverageContributor[];
}

export interface GlobalCapacityPrecheckResult {
  overCapacityFaculty: FacultyOverCapacity[];
  tightCapacityFaculty: FacultyTightCapacity[];
}

export interface CohortPlacementSummary {
  cohortId: number;
  cohortName: string;
  placedCount: number;
  staffedCount: number;
  unplaced: AutoPlaceUnplacedItem[];
  /** True if any of placedCount landed on Saturday — Monday-Friday is always tried first, so this
   *  is a visible "the automation had to overflow into Saturday" fact, not silently absorbed. */
  usedSaturday: boolean;
}

export interface GlobalAutoScheduleResult {
  totalPlaced: number;
  totalStaffed: number;
  cohortSummaries: CohortPlacementSummary[];
  /** Elective-group placement failures — not attributable to a single cohort since a group can
   *  span students from more than one. */
  electiveUnplaced: AutoPlaceUnplacedItem[];
  /** Count of stale over-budget DRAFT sessions the run cleared before placing anything, via a
   *  TEMPORARY backend safety net (see `TimetableGlobalAutoScheduleService#purgeStaleOverBudgetDrafts`).
   *  Always shown when nonzero — never a silent cleanup. */
  staleDraftsCleared: number;
  /** This run's real, exact "still couldn't fill it after trying every eligible faculty" hours —
   *  distinct from `FacultyWorkloadOverviewReport.recommendedAdditionalFacultyCount`'s pre-run
   *  whole-pool estimate, which never reflects real day/period feasibility. 0 when nothing was
   *  genuinely unfillable this run. */
  capacityCausedGapHours: number;
  recommendedAdditionalFacultyCount: number;
  /** LAB/CLINICAL analogue of the self-study capacity gap above: every venue whose own weekly
   *  window capacity (not faculty, not a room/schedule conflict) is why this run couldn't place
   *  everything still short against it. Empty when no venue was the real ceiling this run. */
  venueCapacityGaps: VenueCapacityGap[];
}

/** One Lab or Clinical venue this run genuinely couldn't place enough sessions against because of
 *  its own capacity — see `VenueCapacityGap` (backend) for the full mechanism. Purely informational:
 *  the admin decides whether to raise `currentCapacity` or add a second venue; nothing here is
 *  applied automatically. */
export interface VenueCapacityGap {
  venueId: number;
  venueType: 'LAB' | 'CLINICAL';
  venueName: string;
  currentCapacity: number | null;
  unplacedHours: number;
  affectedSubjectNames: string[];
  /** See `VenueOverCapacity.affectedSubjectIds`. */
  affectedSubjectIds: number[];
}

export interface UnassignedOfferingSummary {
  courseOfferingId: number;
  subjectName: string;
  cohortId: number | null;
  cohortName: string | null;
}

/** One Lab/Clinical venue whose total real weekly demand exceeds its real weekly (day, period)
 *  window — physically cannot fit regardless of arrangement. See backend `VenueOverCapacity`. */
export interface VenueOverCapacity {
  venueId: number;
  venueType: 'LAB' | 'CLINICAL';
  venueName: string;
  capacity: number | null;
  weeklyAvailablePeriods: number;
  weeklyDemandPeriods: number;
  shortfallPeriods: number;
  affectedSubjectNames: string[];
  /** Parallel to `affectedSubjectNames` — passed through to the new venue's create form
   *  (`linkSubjectIds` query param) so saving it immediately makes it eligible for these exact
   *  subjects, closing the gap where a freshly created venue is otherwise invisible to the
   *  suggestion engine until an admin separately edits each Subject. */
  affectedSubjectIds: number[];
}

/** Not over capacity (a run may proceed once acknowledged) but at/near 100% of its weekly window —
 *  real placement isn't guaranteed to succeed even though the raw period totals "fit". */
export interface VenueTightCapacity {
  venueId: number;
  venueType: 'LAB' | 'CLINICAL';
  venueName: string;
  capacity: number | null;
  weeklyAvailablePeriods: number;
  weeklyDemandPeriods: number;
  utilizationPercent: number;
  affectedSubjectNames: string[];
  /** See `VenueOverCapacity.affectedSubjectIds`. */
  affectedSubjectIds: number[];
}

export interface LabClinicalVenueCapacityResult {
  overCapacityVenues: VenueOverCapacity[];
  tightCapacityVenues: VenueTightCapacity[];
}

/** Consolidated "is this ready to automate" report — see backend
 *  {@code TimetableGlobalAutoScheduleService#checkPrerequisites}. General room-commit status is
 *  still checked separately, client-side, against Capacity Planner's own endpoints — Lab/Clinical
 *  venue capacity is the one deliberate exception, since Run Automation itself can fail on it. */
export interface GlobalAutoSchedulePrerequisites {
  offeringsWithoutFaculty: UnassignedOfferingSummary[];
  capacityPrecheck: GlobalCapacityPrecheckResult;
  labClinicalVenueCapacity: LabClinicalVenueCapacityResult;
}
