import { SubstitutionAffectedSection } from '../../academic-year/academic-year.model';
import { TimetableConflictRow } from '../timetable.model';

export type TimetableSessionType = 'THEORY' | 'LAB' | 'CLINICAL' | 'LIBRARY' | 'SPORTS';
export type TimetableCellStatus = 'DRAFT' | 'PUBLISHED';

export interface TimetableSubjectBudget {
  sessionType: TimetableSessionType;
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
  /** Planned against the term's total hours: session occurrences the curriculum hours need across
   *  the term, and what the placed sessions really run (a weekday session every week, a
   *  working-Saturday one only on the chosen Saturdays). Met once delivered reaches required. */
  requiredTermRuns: number;
  deliveredTermRuns: number;
  deliveredHours: number;
}

export interface TimetableSubject {
  courseOfferingId: number;
  subjectName: string;
  subjectCode: string;
  budgets: TimetableSubjectBudget[];
  /** Non-null only for a grouped elective subject — every subject sharing the same group id must
   *  be placed in the same day/period this term (enforced server-side on placement). */
  electiveGroupId: number | null;
  electiveGroupName: string | null;
}

export interface TimetableCell {
  id: number;
  sessionType: TimetableSessionType;
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
  status: TimetableCellStatus;
  /** Non-null only for a cell that's part of a Rotation Group — batchId/batchName are null on
   *  those (there's no single fixed occupant); rotatingBatchNames lists who alternates through it. */
  rotationGroupLabel: string | null;
  rotatingBatchNames: string[];
  /** Null only for a LIBRARY or SPORTS cell — neither has a CourseOffering (see
   *  TimetableGlobalAutoScheduleService#fillLibraryGaps/#fillSportsGaps). Every other session type
   *  always has one. */
  courseOfferingId: number | null;
  subjectName: string;
  subjectCode: string;
  electiveGroupId: number | null;
  electiveGroupName: string | null;
  /** True for an institution-decided elective: only its chosen option runs, as a common cohort
   *  subject, so it moves, swaps and is replaced like any subject. A student-choice elective
   *  (false) keeps every option in one shared slot, which only Run Automation moves. */
  commonElective: boolean;
  /** Non-null only for a cell that's part of a multi-period session (periodSpan) — every sibling
   *  cell sharing this id was placed/staffed/removed together as one atomic unit. */
  sessionGroupId: string | null;
  /** True when a human positioned this cell on purpose (manual place, drag-move, swap, or an
   *  explicit pin). A pinned cell survives the next Global Auto-Schedule rebuild — automation packs
   *  the rest of the week around it instead of clearing it. */
  pinned: boolean;
  /** True when this cell's subject is curriculum-typed CO_CURRICULAR (e.g. Self-Study) — advisory
   *  content the auto-scheduler always places last, never a real curriculum requirement. False for
   *  LIBRARY/SPORTS too (no CourseOffering to type at all — the grid colors those as their own
   *  fixed categories, not this one). Drives the grid's cell coloring. */
  coCurricular: boolean;
}

export interface TimetableBatchOption {
  id: number;
  courseOfferingId: number;
  name: string;
  capacity: number;
  enrolledCount: number;
}

/** One committed Theory room/section a THEORY placement can target — mirrors the backend's
 *  CohortSectionResponse (reused directly there rather than a duplicated shape). */
export interface TimetableSectionOption {
  id: number;
  sectionLabel: string;
  classroomId: number;
  classroomName: string;
  classroomCapacity: number;
  plannedSize: number;
  isActive: boolean;
}

/** One active Clinical Shift Group's real Clinical hours for the whole term — already converted
 *  from the offering's configured shift duration × weeksInTerm server-side. Clinical Shift
 *  sessions bypass the period grid entirely (real clock times, no ClassSchedule row), so they
 *  never appear in `cells` — this is how the hours-assigned card learns about them instead of
 *  silently under-counting Clinical for any cohort using Clinical Shift Groups. */
export interface TimetableClinicalShiftHours {
  courseOfferingId: number;
  cohortSectionId: number | null;
  assignedHours: number;
}

/** A Clinical Shift Group's wall-clock window, including bus travel buffer — all times are
 *  `HH:mm:ss` strings. Used to widen the grid's displayed time range and render the shift block;
 *  the actual period-level hard block is already enforced server-side, so this is purely a
 *  rendering aid. */
export interface ClinicalShiftWindow {
  shiftGroupId: number;
  label: string;
  dayOfWeek: string;
  busDepart: string | null;
  clinicalStart: string;
  clinicalEnd: string | null;
  busReturn: string | null;
}

/** Cohort-wide since R3.1 — one response covers every non-elective subject a cohort has in a
 *  term, merging their cells/batches so cross-subject placement conflicts are visible in a
 *  single grid instead of hidden behind a per-subject filter. `sections` lists the cohort's
 *  active Cohort Room Allocation sections for this term (empty if none committed).
 *  `weeksInTerm`/`workingSaturdayCount` are term-wide constants (not per-subject) used to compute
 *  an honest scheduled-hours total from `cells`: a Mon-Fri cell recurs `weeksInTerm` times, a
 *  Saturday-placed one only recurs `workingSaturdayCount` times (0 if no working-Saturday pattern
 *  is configured for the term — Saturday is opt-in, off by default). */
export interface TimetableBuilderResponse {
  cohortId: number;
  cohortName: string;
  termInstanceLabel: string;
  subjects: TimetableSubject[];
  cells: TimetableCell[];
  batches: TimetableBatchOption[];
  sections: TimetableSectionOption[];
  weeksInTerm: number;
  workingSaturdayCount: number;
  clinicalShiftHours: TimetableClinicalShiftHours[];
  /** True once this term's timetable has been approved/PUBLISHED on Draft Review — a term-wide
   *  fact (same for every cohort in the term), not a per-cohort one, and distinct from whether
   *  this cohort's own Cohort Room Allocation is committed (`sections` non-empty). Past this point
   *  Global Auto-Schedule refuses to run; only manual period/staff edits remain available. */
  termTimetablePublished: boolean;
  /** This cohort's active Clinical Shift wall-clock windows, per day — see {@link ClinicalShiftWindow}. */
  clinicalShiftWindows: ClinicalShiftWindow[];
}

export interface TimetableCellPlacementRequest {
  courseOfferingId: number;
  sessionType: TimetableSessionType;
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

export interface TimetablePlacementCandidate {
  dayOfWeek: string;
  periodId: number;
}

/** One grid slot's live legality for dragging a specific already-placed cell there — powers the
 *  drag-highlight preview. `reason` is a human-readable violation message when `valid` is false
 *  (matching the backend's first-violation-wins order), null when valid. */
export interface TimetableSlotPreview {
  dayOfWeek: string;
  periodId: number;
  valid: boolean;
  reason: string | null;
}

export interface TimetableCellMoveRequest {
  dayOfWeek: string;
  periodId: number;
  cohortId: number;
}

/** Atomically exchanges two already-placed DRAFT cells' day/period — fired instead of a plain
 *  move when a drag lands on a slot that's already occupied by exactly one other cell. */
export interface TimetableCellSwapRequest {
  targetCellId: number;
  cohortId: number;
}

/** One session (a single period or a whole block, with every parallel batch that moves alongside
 *  it) that a relocation or Clinical duty-day change would move — a before → after preview row. */
export interface TimetablePlannedMove {
  subjectCode: string;
  sessionType: TimetableSessionType;
  /** Section or batch names, e.g. "Section 1" or "Batch A, Batch B". */
  occupantLabel: string | null;
  fromDay: string;
  fromPeriodIds: number[];
  toDay: string;
  toPeriodIds: number[];
}

/** Whether a session could go, whole block included, to the same-length window starting at
 *  dayOfWeek/startPeriodId: MOVE into empty periods, SWAP with the sessions there (they take its
 *  periods), or invalid with the reason. `moves` lists the dragged session first. */
export interface TimetableRelocationPlan {
  dayOfWeek: string;
  startPeriodId: number;
  periodIds: number[];
  kind: 'MOVE' | 'SWAP' | null;
  valid: boolean;
  reason: string | null;
  moves: TimetablePlannedMove[];
}

export interface TimetableRelocateRequest {
  dayOfWeek: string;
  startPeriodId: number;
  cohortId: number;
}

/** Whether a Clinical duty could move to this day, and which sessions would swap into the day it leaves. */
export interface DutyDayMovePreview {
  dayOfWeek: string;
  valid: boolean;
  reason: string | null;
  moves: TimetablePlannedMove[];
}

export interface DutyDayMoveRequest {
  dayOfWeek: string;
  cohortId: number;
}

/** Hands a placed Theory cell's slot to a different subject, keeping its day/period/audience.
 *  THEORY only, DRAFT only, and never an elective on either side — an elective group shares one
 *  slot across all its members, so it's re-placed via Place Elective Block instead. */
export interface TimetableCellReplaceRequest {
  courseOfferingId: number;
  facultyId: number;
}

/** How far below its curriculum Theory hours the subject we just displaced now sits across the
 *  term, for this exact section. `shortfallHours` is always above 0 when present — it's what still
 *  needs re-placing elsewhere in the week. Mirrors the backend's
 *  `TimetableCellReplaceResponse.DisplacedSubjectShortfall` exactly. */
export interface DisplacedSubjectShortfall {
  courseOfferingId: number;
  subjectName: string;
  subjectCode: string;
  cohortSectionId: number | null;
  cohortSectionLabel: string | null;
  requiredHours: number;
  placedHours: number;
  shortfallHours: number;
}

/** `displaced` is null when the replacement cost nothing that matters: the cell had no previous
 *  offering (a Library cell), the previous subject has no curriculum hours to measure against, or
 *  it still meets its requirement without this slot (it was over quota, or covered elsewhere). */
export interface TimetableCellReplaceResponse {
  cell: TimetableCell;
  displaced: DisplacedSubjectShortfall | null;
}

export interface AutoPlaceUnplacedItem {
  subjectName: string;
  sessionType: TimetableSessionType;
  occupantLabel: string | null;
  reason: string;
  /** Null only for a whole-elective-group failure (no single offering to point at) — used to
   *  deep-link a Special Class request pre-filled with the right subject. */
  courseOfferingId: number | null;
  /** True only when curriculum hours went unplaced because the week had no free slot left — the
   *  one kind more working Saturdays or a Special Class can close. Library/idle-batch fallbacks,
   *  Self-Study/gap-fill notes, a missing faculty or elective selection, and a room ceiling are false. */
  slotShortfall: boolean;
  /** OC-256 follow-up: true for Library/Sports/Self-Study/idle-batch-fallback filler, which has no
   *  curriculum-hours budget and was never required to place — false for a real Theory/Lab/Clinical
   *  shortfall. Render advisory items muted (like infoNotes), never with the same alarming style as
   *  a genuine gap — that conflation is what made a fully-covered cohort's run look broken. */
  advisoryOnly: boolean;
}


export interface ElectiveGroupMemberPlacement {
  courseOfferingId: number;
  sessionType: TimetableSessionType;
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
  /** True if any of placedCount landed on Saturday — a regular working day whenever the term has
   *  chosen working Saturdays, counted for the runs it really has. Informational only. */
  usedSaturday: boolean;
  /** Neutral, expected outcomes (e.g. Library shrinking to fit a full week) — shown grey, never as
   *  warnings. */
  infoNotes: string[];
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
  /** How many DRAFT sessions the rebuild deliberately LEFT standing because they were pinned.
   *  Surfaced alongside `staleDraftsCleared` so a run is explicit about both halves of what it did
   *  to the existing grid — kept vs cleared. */
  pinnedCellsPreserved: number;
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
  /** Every cohort this "All Cohorts" run deliberately left untouched because THAT cohort's own
   *  timetable is already approved/PUBLISHED on Draft Review — once approved, only manual
   *  period/staff edits (swap staff, swap sessions) are allowed, never a full automated re-run.
   *  Approve is cohort-scoped (OC-258/OC-260), so this can be a genuine partial list — other cohorts
   *  enrolled in the same term instance (e.g. a different year-group of the same program) may still
   *  be Pending and get placed normally in the same run. Always empty for a single-cohort run (that
   *  case is a hard block at the API boundary instead — see `canRunAutoSchedule` in
   *  timetable-builder.component.ts). */
  skippedPublishedCohorts: SkippedPublishedCohort[];
  /** How many cross-offering LAB pairings this run turned into a real RotationGroup (two offerings
   *  sharing one cohort section, each split into exactly 2 batches on its own Lab, alternated
   *  week-to-week instead of leaving the "off-duty" batch idle). 0 on a run with no eligible pair —
   *  never an error; most offerings simply aren't this exact shape. */
  rotationGroupsCreated: number;
  /** Every candidate pair Phase B considered but couldn't actually place (no shared free day/period
   *  found for both labs, all batches, and both faculty) — distinct from a pair simply never being
   *  eligible (mismatched batch counts, different block sizes), which is silent by design. */
  pairingSkipReasons: string[];
  /** Every subject where this run had to place one or more sessions with a different,
   *  already-eligible faculty member instead of the offering's own bound faculty, because that bound
   *  faculty was unavailable at every remaining slot. The substitute sessions are already placed and
   *  staffed by the time this is reported — purely an actionable "consider reassigning this offering"
   *  tip, never a pending action. Empty on the common run where every row's own bound faculty covered
   *  everything it needed to. */
  facultySubstitutionTips: FacultySubstitutionTip[];
  /** Curriculum Clinical hours a shift-configured subject still owes after BOTH its duty roster and
   *  the weekly grid have delivered everything they structurally can. This is arithmetic, not a
   *  placement failure: a duty group runs one occurrence a week, so three duty days over a 26-week
   *  term give 78 occurrences while a 480h subject at a 6h shift needs 80. The 12h left over is two
   *  duty DAYS, and the grid cannot express it — its smallest weekly clinical row would deliver
   *  ~86.7h against 12h owed. The run declines that row rather than overshooting by ~75h, and
   *  reports the remedy instead. Empty on a term whose duty rosters already cover their subjects. */
  clinicalResiduals: ClinicalResidualItem[];
  /** A term-wide conflict scan (the same check Conflict Inspector runs) taken right after this run
   *  finished placing/staffing everything it could — flag-only, nothing here was auto-resolved. A
   *  pinned cell is deliberately left standing rather than re-placed, so a conflict it has with
   *  something this run just placed (or with another cohort's own pre-existing cell) would
   *  otherwise go unnoticed until someone happened to open Conflict Inspector separately. Empty on
   *  a term with no structural conflicts left standing after this run. */
  postRunConflicts: TimetableConflictRow[];
}

/** One subject's leftover Clinical hours and the duty days that close them — see {@link
 *  GlobalAutoScheduleResult.clinicalResiduals}. */
export interface ClinicalResidualItem {
  courseOfferingId: number;
  subjectName: string;
  cohortName: string | null;
  residualHours: number;
  hoursPerDutyDay: number;
  extraDutyDays: number;
  remedy: string;
  currentDurationMinutes: number | null;
  /** The other remedy (OC-227): the shortest duty length at which the existing roster alone delivers
   *  every curriculum Clinical hour. Null when lengthening the duty can't help. */
  suggestedDurationMinutes: number | null;
  /** True when that longer duty costs zero timetable periods (students are still back before any
   *  period that's free today). */
  suggestedDurationCostsNoPeriods: boolean;
}

/** One subject this run had to fall back off {@code originalFacultyName} onto {@code
 *  substituteFacultyName} to actually place {@code sessionCount} session(s) — see {@link
 *  GlobalAutoScheduleResult.facultySubstitutionTips}. `substituteRemainingHours`/
 *  `substituteCapacityTier` are the substitute's own term workload BEFORE this run added anything
 *  to them (null/'NONE' means no cap is configured for them at all — never read null as "no
 *  capacity left"). `substituteTotalSessionsThisRun` is that same substitute's grand total across
 *  EVERY subject they picked up as a fallback this run — can exceed this tip's own `sessionCount`
 *  if they covered more than one subject, and is the number to actually check before trusting them
 *  as a permanent reassignment. */
export interface FacultySubstitutionTip {
  subjectName: string;
  originalFacultyId: number;
  originalFacultyName: string;
  substituteFacultyId: number;
  substituteFacultyName: string;
  sessionCount: number;
  courseOfferingId: number;
  substituteRemainingHours: number | null;
  substituteCapacityTier: string;
  substituteTotalSessionsThisRun: number;
  /** The exact (cohort, section) rows this tip's own fallback covered — pass through unchanged to
   *  `ConfirmFacultySubstitutionItem` on Submit so confirming this tip never reassigns a sibling
   *  section a different, separately-ticked tip already claimed. */
  affectedSections: SubstitutionAffectedSection[];
}

/** One cohort excluded from an "All Cohorts" run because this term's timetable is already
 *  approved/PUBLISHED. */
export interface SkippedPublishedCohort {
  cohortId: number;
  cohortName: string;
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

/** One cohort/day combination where an active Clinical Shift window leaves too few (or zero) real
 *  on-campus periods free for Theory/Lab that day. See backend `ClinicalShiftDayShortfall`. */
export interface ClinicalShiftDayShortfall {
  cohortId: number;
  cohortDisplayName: string;
  dayOfWeek: string;
  totalActivePeriods: number;
  periodsBlockedByShift: number;
  periodsFreeForTheoryLab: number;
  affectedShiftLabels: string[];
}

/** Two-tier Clinical-Shift-vs-Period-grid feasibility report, mirroring
 *  `LabClinicalVenueCapacityResult`'s over/tight split — `zeroPeriodDays` is a hard block,
 *  `tightPeriodDays` is a non-blocking warning. Only ever populated for cohorts whose Program has
 *  opted into Clinical Shift scheduling. */
export interface ClinicalShiftPeriodAvailabilityResult {
  zeroPeriodDays: ClinicalShiftDayShortfall[];
  tightPeriodDays: ClinicalShiftDayShortfall[];
}

/** Consolidated "is this ready to automate" report — see backend
 *  {@code TimetableGlobalAutoScheduleService#checkPrerequisites}. General room-commit status is
 *  still checked separately, client-side, against Capacity Planner's own endpoints — Lab/Clinical
 *  venue capacity is the one deliberate exception, since Run Automation itself can fail on it. */
export interface GlobalAutoSchedulePrerequisites {
  offeringsWithoutFaculty: UnassignedOfferingSummary[];
  capacityPrecheck: GlobalCapacityPrecheckResult;
  labClinicalVenueCapacity: LabClinicalVenueCapacityResult;
  clinicalShiftPeriodAvailability: ClinicalShiftPeriodAvailabilityResult;
}
