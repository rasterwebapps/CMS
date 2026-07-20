import { WeekGridSession } from '../../shared/week-grid/week-grid.model';

export type ClassSchedule = WeekGridSession;

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
