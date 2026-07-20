export type ClassSessionType = 'THEORY' | 'LAB';
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
  labSlotId: number | null;
  periodId: number | null;
  slotName: string;
  startTime: string;
  endTime: string;
  batchName: string | null;
  batchId: number | null;
  classroomId: number | null;
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
  labSlotId?: number | null;
  batchName?: string | null;
  batchId?: number | null;
  dayOfWeek: string;
  termInstanceId: number;
  isActive?: boolean;
  classroomId?: number | null;
  periodId?: number | null;
  courseOfferingId?: number | null;
}

export interface LabSlot {
  id: number;
  name: string;
  startTime: string;
  endTime: string;
  slotOrder: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LabSlotRequest {
  name: string;
  startTime: string;
  endTime: string;
  slotOrder?: number;
  isActive?: boolean;
}

export const DAYS_OF_WEEK = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
