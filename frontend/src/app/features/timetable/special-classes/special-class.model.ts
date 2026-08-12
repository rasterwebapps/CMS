export type OccurrenceSource = 'REGULAR' | 'SPECIAL_CLASS' | 'DAY_REPEAT';
export type SpecialClassApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
export type SpecialClassSessionType = 'THEORY' | 'LAB' | 'CLINICAL';
export type WeekDay = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY';

/** BR-55: mirrors backend `SpecialClassOccurrenceDto`. */
export interface SpecialClassOccurrence {
  id: number;
  occurrenceSource: OccurrenceSource;
  occurrenceDate: string;
  subjectId: number | null;
  subjectName: string | null;
  subjectCode: string | null;
  courseOfferingId: number | null;
  cohortSectionId: number | null;
  cohortSectionLabel: string | null;
  periodId: number | null;
  periodName: string | null;
  periodStartTime: string | null;
  periodEndTime: string | null;
  sessionType: SpecialClassSessionType;
  venueId: number | null;
  venueName: string | null;
  requestedFacultyId: number | null;
  requestedFacultyName: string | null;
  approvalStatus: SpecialClassApprovalStatus;
  requestedByFacultyId: number | null;
  requestedByFacultyName: string | null;
  requestedAt: string | null;
  requestReason: string | null;
  sourceDayOfWeek: WeekDay | null;
  requestBatchId: string | null;
  approvedBy: string | null;
  approvedAt: string | null;
  rejectionReason: string | null;
}

/** Mirrors backend `SpecialClassRequest`. Exactly one of classroomId/labId/clinicalVenueId must
 *  be set, matching sessionType. */
export interface SpecialClassRequestPayload {
  occurrenceDate: string;
  periodId: number;
  subjectId: number;
  courseOfferingId: number;
  cohortSectionId: number | null;
  sessionType: SpecialClassSessionType;
  classroomId: number | null;
  labId: number | null;
  clinicalVenueId: number | null;
  requestedFacultyId: number;
  reason: string | null;
}

/** Mirrors backend `DayRepeatRequest`. */
export interface DayRepeatRequestPayload {
  termInstanceId: number;
  sourceDayOfWeek: WeekDay;
  targetDate: string;
  cohortSectionId: number;
  reason: string | null;
}

/** Mirrors backend `DayRepeatResult`. */
export interface DayRepeatResult {
  created: SpecialClassOccurrence[];
  skippedCount: number;
}
