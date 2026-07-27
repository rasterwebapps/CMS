import { WeekGridCandidateCell, WeekGridSession } from '../../shared/week-grid/week-grid.model';

export type ClassSchedule = WeekGridSession;
export type SwapCandidate = WeekGridCandidateCell;

export interface SwapTarget {
  dayOfWeek: string;
  periodId?: number | null;
  labSlotId?: number | null;
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
  holidayDayIndexes: number[];
}
