export type ClassSessionType = 'THEORY' | 'LAB' | 'CLINICAL';
export type ClassScheduleStatus = 'DRAFT' | 'PUBLISHED';

export interface LabSchedule {
  id: number;
  sessionType: ClassSessionType;
  status: ClassScheduleStatus;
  labId: number | null;
  labName: string | null;
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  facultyId: number;
  facultyName: string;
  periodId: number | null;
  slotName: string;
  startTime: string;
  endTime: string;
  batchName: string | null;
  batchId: number | null;
  classroomId: number | null;
  clinicalVenueId: number | null;
  roomName: string;
  courseOfferingId: number | null;
  dayOfWeek: string;
  termInstanceId: number;
  termInstanceLabel: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LabScheduleRequest {
  sessionType: ClassSessionType;
  labId?: number | null;
  subjectId: number;
  facultyId: number;
  batchName?: string | null;
  batchId?: number | null;
  dayOfWeek: string;
  termInstanceId: number;
  isActive?: boolean;
  classroomId?: number | null;
  periodId?: number | null;
  clinicalVenueId?: number | null;
  courseOfferingId?: number | null;
}

export const DAYS_OF_WEEK = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
