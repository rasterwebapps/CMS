export type OccurrenceStatus = 'HELD' | 'SUBSTITUTED' | 'CANCELLED';

export interface FacultyAbsence {
  id: number;
  facultyId: number;
  facultyName: string;
  absenceDate: string;
  reason: string | null;
  recordedBy: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface FacultyAbsenceRequest {
  facultyId: number;
  absenceDate: string;
  reason?: string | null;
}

export interface AffectedSession {
  classScheduleId: number;
  subjectName: string;
  subjectCode: string;
  roomName: string | null;
  slotName: string | null;
  startTime: string;
  endTime: string;
  batchName: string | null;
  occurrenceStatus: OccurrenceStatus;
  substituteFacultyName: string | null;
}

export interface SubstituteCandidate {
  facultyId: number;
  facultyName: string;
}
