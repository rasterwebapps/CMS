export interface TimetableConstraintViolation {
  code: string;
  message: string;
}

export interface TimetableConflictRow {
  classScheduleId: number;
  subjectName: string;
  subjectCode: string;
  sessionType: 'THEORY' | 'LAB' | 'CLINICAL';
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

export interface ConflictScanResponse {
  termInstanceId: number;
  termLabel: string;
  scannedAt: string;
  scannedCellCount: number;
  violationCellCount: number;
  violationCount: number;
  countsByCode: Record<string, number>;
  rows: TimetableConflictRow[];
}
