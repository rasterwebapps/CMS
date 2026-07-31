export type WeekGridSessionType = 'THEORY' | 'LAB' | 'CLINICAL';
export type WeekGridSessionStatus = 'DRAFT' | 'PUBLISHED';

/** Session-type-neutral shape a week-grid cell renders — matches the backend's
 *  ClassScheduleResponse fields that stay meaningful regardless of THEORY vs LAB. */
export interface WeekGridSession {
  id: number;
  sessionType: WeekGridSessionType;
  status: WeekGridSessionStatus;
  subjectName: string;
  subjectCode: string;
  /** Null for an unstaffed R3 Phase 4 skeleton row -- faculty/room are assigned later by the
   *  R3 Phase 5 Staffing screen. */
  facultyName: string | null;
  roomName: string | null;
  batchName: string | null;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  slotName: string;
}

export type WeekGridMode = 'review' | 'personal' | 'browse';

/** A valid swap-mode target cell — day+slot combo the currently selected session could move
 *  into, sourced from the backend's swap-candidates endpoint (matches SwapCandidate 1:1). */
export interface WeekGridCandidateCell {
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  periodId: number | null;
  occupied: boolean;
  occupyingSessionId: number | null;
  occupyingSubjectName: string | null;
}

export type WeekGridHolidayCategory = 'GOVERNMENT' | 'LOCAL' | 'INSTITUTIONAL';

/** One holiday overlapping a viewed week — dayIndex is 0=Monday .. 5=Saturday, matching
 *  WEEK_GRID_DAYS order. category is null for holidays created before HolidayCategory existed,
 *  or left "Unspecified" on the calendar event. */
export interface WeekGridHolidayInfo {
  dayIndex: number;
  title: string;
  category: WeekGridHolidayCategory | null;
}

export const WEEK_GRID_HOLIDAY_CATEGORY_LABELS: Record<WeekGridHolidayCategory, string> = {
  GOVERNMENT: 'Government',
  LOCAL: 'Local',
  INSTITUTIONAL: 'Institutional',
};

export const WEEK_GRID_DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
export const WEEK_GRID_DAY_LABELS: Record<string, string> = {
  MONDAY: 'Mon', TUESDAY: 'Tue', WEDNESDAY: 'Wed', THURSDAY: 'Thu', FRIDAY: 'Fri', SATURDAY: 'Sat',
};
