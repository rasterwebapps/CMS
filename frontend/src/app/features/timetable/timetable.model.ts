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
  sessionType: 'THEORY' | 'LAB' | 'CLINICAL' | 'LIBRARY';
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
