export interface Attendance {
  id: number;
  studentId: number;
  studentName: string;
  subjectId: number;
  subjectName: string;
  date: string;
  status: string;
  type: string;
  createdAt: string;
  updatedAt: string;
}

export interface AttendanceRequest {
  studentId: number;
  subjectId: number;
  date: string;
  status: string;
  type: string;
}

export interface BulkAttendanceRequest {
  subjectId: number;
  date: string;
  type: string;
  studentAttendances: Array<{ studentId: number; status: string }>;
}

export interface AvailableSubject {
  classScheduleId: number;
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  batchName: string | null;
  slotName: string | null;
  startTime: string | null;
  endTime: string | null;
}

export interface AttendanceReport {
  studentId: number;
  studentName: string;
  totalClasses: number;
  present: number;
  absent: number;
  attendancePercentage: number;
}
