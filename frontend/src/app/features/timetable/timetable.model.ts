import { WeekGridCandidateCell, WeekGridHolidayInfo, WeekGridSession } from '../../shared/week-grid/week-grid.model';

export type ClassSchedule = WeekGridSession;
export type SwapCandidate = WeekGridCandidateCell;

export interface SwapTarget {
  dayOfWeek: string;
  periodId?: number | null;
}

export interface TimetableActionResponse {
  affectedCount: number;
}

/** Per-cohort conflict-acknowledgment status (Timetable Builder's "Check & Resolve Conflicts"
 *  row action) -- OC-260's replacement for the retired term-wide Conflict Inspector screen. */
export interface ConflictAcknowledgmentStatus {
  termInstanceId: number;
  acknowledged: boolean;
  acknowledgedAt: string | null;
}

export interface TimetableConstraintViolation {
  code: string;
  message: string;
}

/** One scanned session's structural-violation result, as returned by the backend's whole-term
 *  conflict scan (still used internally by Global Auto-Schedule's post-run report). */
export interface TimetableConflictRow {
  classScheduleId: number;
  subjectName: string;
  subjectCode: string;
  sessionType: 'THEORY' | 'LAB' | 'CLINICAL' | 'LIBRARY' | 'SPORTS';
  dayOfWeek: string;
  periodLabel: string | null;
  startTime: string;
  endTime: string;
  facultyName: string | null;
  venueName: string | null;
  cohortLabel: string | null;
  status: 'DRAFT' | 'PUBLISHED';
  violations: TimetableConstraintViolation[];
}

/** One cohort/session-type combination Approve found with curriculum-required hours never placed
 *  as real sessions for the term (OC-256) -- the same figure Timetable Builder's own "Total
 *  Unassigned" stat cards already show that cohort, surfaced here from
 *  TimetableCoverageGapException's `gaps` so Draft Review can list exactly what's missing before
 *  offering an override. */
export interface TimetableCoverageGap {
  cohortId: number;
  cohortName: string;
  sessionType: 'THEORY' | 'LAB' | 'CLINICAL';
  totalHours: number;
  assignedHours: number;
  unassignedHours: number;
}

/** One cohort/section's real per-week Clinical hours delivered off-grid via an active Clinical
 *  Shift Group (duty roster) -- these never produce a grid cell, so Draft Review surfaces them as
 *  a banner instead of leaving the cohort's Clinical component looking simply missing. */
export interface ClinicalShiftSummaryItem {
  cohortSectionId: number;
  cohortLabel: string;
  hoursPerWeek: number;
}

/** OC-260: a cohort's own position in the Pending -> Draft/Generated -> Conflicts Resolved ->
 *  Published lifecycle Timetable Builder's status badge and row actions are both driven from.
 *  "PENDING" means no sessions have been placed for this cohort/term yet. "CONFLICTS_RESOLVED"
 *  means every one of Approve's preflight gates (staffing, offering-assignment, conflict scan,
 *  coverage, and a fresh per-cohort conflict acknowledgment) currently passes for this cohort --
 *  computed server-side from the same checks Approve itself enforces, so this can never say
 *  "ready" when Publish would actually still fail. */
export type CohortReadinessStatus = 'PENDING' | 'DRAFTED' | 'CONFLICTS_RESOLVED' | 'PUBLISHED' | 'PARTIALLY_PUBLISHED';

/** One row of Timetable Builder's "All cohorts" landing summary table (OC-260 folded the former
 *  Draft Review screen's own identical table in here) -- a cohort's aggregate lifecycle status for
 *  a term instance, synthesized server-side from its sessions' DRAFT/PUBLISHED status plus the same
 *  gate checks Approve enforces; never a persisted value itself. "PARTIALLY_PUBLISHED" reflects the
 *  real, already-existing scenario where a post-publish edit (Staff Session Swap, an individual
 *  Timetable Builder placement) creates new DRAFT rows alongside already-PUBLISHED rows for the
 *  same cohort/term. */
export interface CohortTermStatusSummary {
  cohortId: number;
  cohortName: string;
  courseName: string | null;
  admissionYearName: string | null;
  status: CohortReadinessStatus;
  draftCount: number;
  publishedCount: number;
  /** Curriculum-required THEORY/LAB/CLINICAL hours not yet placed as real sessions (0 = fully
   *  covered) -- the same figure that gates Publish. */
  unassignedHours: number;
  /** True once attendance has been recorded against any of this cohort's sessions for this term --
   *  Discard and Revert-to-Draft both permanently refuse once this is true, so the table hides
   *  those actions for this row instead of offering a button that can only ever fail. */
  attendanceRecorded: boolean;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface MyTimetableResponse {
  sessions: ClassSchedule[];
  holidays: WeekGridHolidayInfo[];
}

export type OccurrenceStatus = 'HELD' | 'SUBSTITUTED' | 'CANCELLED';

/** One calendar-dated firing of a recurring ClassSchedule row -- the projection behind
 *  Month/Week/Day calendar views, from GET /timetables/occurrences. occurrenceStatus is CANCELLED
 *  (with cancelReason set) for a date the session's period is blocked -- shown explicitly rather
 *  than silently missing from the list. */
export interface ClassScheduleOccurrence {
  date: string;
  session: ClassSchedule;
  occurrenceStatus: OccurrenceStatus;
  cancelReason: string | null;
}

export type TimetableOccurrenceScope = 'browse' | 'personal';

export interface ResourceGridCell {
  sessionId: number;
  subjectName: string;
  subjectCode: string;
  roomName: string;
  facultyName: string;
  batchName: string | null;
  startTime: string;
  endTime: string;
  slotName: string;
  sessionType: 'THEORY' | 'LAB' | 'CLINICAL' | 'LIBRARY' | 'SPORTS';
  status: 'DRAFT' | 'PUBLISHED';
  /** The course offering's curriculum term/semester number (e.g. 1, 3, 5) — this grid pools
   *  sessions from every active cohort's own faculty/room onto one row, so this is what tells two
   *  cohorts' sessions apart at a glance. Null only if the backing session has no offering link. */
  termNumber: number | null;
  /** True only for a synthetic Clinical Shift cell (bus-depart through bus-return) — it has no
   *  backing ClassSchedule row, so `sessionId` is a negative, non-clickable placeholder id. */
  isOffCampusShift: boolean;
  /** The column this cell renders under — see the backend's identically-named field on
   *  ResourceGridCellResponse for what this means in Date mode vs Weekday mode. */
  dayOfWeek: string;
  /** Null for a synthetic Clinical Shift cell — matches WeekGridSession's own null-periodId
   *  convention for an off-grid entry that cms-week-grid renders as a spanning block. */
  periodId: number | null;
}

export interface ResourceGridRow {
  resourceId: number;
  resourceName: string;
  sessions: ResourceGridCell[];
}

export type ResourceGridType = 'FACULTY' | 'CLASSROOM';

export interface StaffSwapCandidate {
  classScheduleId: number;
  subjectName: string;
  facultyName: string;
  startTime: string;
  endTime: string;
}
