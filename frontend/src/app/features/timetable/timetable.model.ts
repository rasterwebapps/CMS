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

/** One cohort/session-type combination Approve found with curriculum-required hours never placed
 *  as real sessions for the term (OC-256) -- the same figure Skeleton Builder's own "Total
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

export type CohortTermStatus = 'DRAFT' | 'PUBLISHED' | 'PARTIALLY_PUBLISHED';

/** One row of Draft Review's landing summary table -- a cohort's aggregate publish status for a
 *  term instance, synthesized server-side from its sessions' DRAFT/PUBLISHED status; never a
 *  persisted value itself. "PARTIALLY_PUBLISHED" reflects the real, already-existing scenario
 *  where a post-publish edit (Staff Session Swap, an individual Skeleton Builder placement)
 *  creates new DRAFT rows alongside already-PUBLISHED rows for the same cohort/term. */
export interface CohortTermStatusSummary {
  cohortId: number;
  cohortName: string;
  courseName: string | null;
  admissionYearName: string | null;
  status: CohortTermStatus;
  draftCount: number;
  publishedCount: number;
  /** Curriculum-required THEORY/LAB/CLINICAL hours not yet placed as real sessions (0 = fully
   *  covered) -- the same figure that gates Publish. */
  unassignedHours: number;
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
  /** True only for a synthetic Clinical Shift cell (bus-depart through bus-return) — it has no
   *  backing ClassSchedule row, so `sessionId` is a negative, non-clickable placeholder id. */
  isOffCampusShift: boolean;
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
