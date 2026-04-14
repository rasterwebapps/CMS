export interface LabSchedule {
  id: number;
  labId: number;
  labName: string;
  courseId: number;
  courseName: string;
  courseCode: string;
  facultyId: number;
  facultyName: string;
  labSlotId: number;
  labSlotName: string;
  startTime: string;
  endTime: string;
  batchName: string;
  dayOfWeek: string;
  semesterId: number;
  semesterName: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LabScheduleRequest {
  labId: number;
  courseId: number;
  facultyId: number;
  labSlotId: number;
  batchName: string;
  dayOfWeek: string;
  semesterId: number;
  isActive?: boolean;
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
