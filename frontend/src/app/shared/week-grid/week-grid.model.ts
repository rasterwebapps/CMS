export type WeekGridSessionType = 'THEORY' | 'LAB';
export type WeekGridSessionStatus = 'DRAFT' | 'PUBLISHED';

/** Session-type-neutral shape a week-grid cell renders — matches the backend's
 *  ClassScheduleResponse fields that stay meaningful regardless of THEORY vs LAB. */
export interface WeekGridSession {
  id: number;
  sessionType: WeekGridSessionType;
  status: WeekGridSessionStatus;
  subjectName: string;
  subjectCode: string;
  facultyName: string;
  roomName: string;
  batchName: string | null;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  slotName: string;
}

export type WeekGridMode = 'review' | 'personal' | 'browse';

export const WEEK_GRID_DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
export const WEEK_GRID_DAY_LABELS: Record<string, string> = {
  MONDAY: 'Mon', TUESDAY: 'Tue', WEDNESDAY: 'Wed', THURSDAY: 'Thu', FRIDAY: 'Fri', SATURDAY: 'Sat',
};
