export type StaffingSessionType = 'THEORY' | 'LAB' | 'CLINICAL';

export interface UnstaffedCell {
  id: number;
  courseOfferingId: number | null;
  subjectName: string;
  subjectCode: string;
  subjectSpecialityId: number | null;
  subjectSpecialityName: string | null;
  sessionType: StaffingSessionType;
  dayOfWeek: string;
  periodId: number;
  slotName: string;
  startTime: string;
  endTime: string;
  batchName: string | null;
}

export interface StaffingAssignmentRequest {
  facultyId: number;
  classroomId: number | null;
  labId: number | null;
  clinicalVenueId: number | null;
}
