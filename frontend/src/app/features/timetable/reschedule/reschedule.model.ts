export interface ApplyRescheduleRequestPayload {
  date: string;
  targetDate: string;
  periodId: number;
  venueId: number;
}

export interface RescheduleResponse {
  classScheduleId: number;
  date: string;
  targetDate: string;
  periodName: string | null;
  venueName: string | null;
  occurrenceStatus: string;
}
