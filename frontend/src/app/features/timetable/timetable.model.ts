import { WeekGridCandidateCell, WeekGridHolidayInfo, WeekGridSession } from '../../shared/week-grid/week-grid.model';

export type ClassSchedule = WeekGridSession;
export type SwapCandidate = WeekGridCandidateCell;

export interface SwapTarget {
  dayOfWeek: string;
  periodId?: number | null;
}

export interface TimetableGenerationResponse {
  generatedCount: number;
  unplaceable: string[];
}

export interface TimetableActionResponse {
  affectedCount: number;
}

export interface MyTimetableResponse {
  sessions: ClassSchedule[];
  holidays: WeekGridHolidayInfo[];
}

/** One real calendar-dated firing of a recurring ClassSchedule row -- the projection behind
 *  Month/Week/Day calendar views, from GET /timetables/occurrences. */
export interface ClassScheduleOccurrence {
  date: string;
  session: ClassSchedule;
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
  sessionType: 'THEORY' | 'LAB' | 'CLINICAL';
  status: 'DRAFT' | 'PUBLISHED';
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
