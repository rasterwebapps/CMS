export interface Attendance {
  id: number;
  studentId: number;
  studentName: string;
  courseId: number;
  courseName: string;
  date: string;
  status: string;
  type: string;
  createdAt: string;
  updatedAt: string;
}

export interface AttendanceRequest {
  studentId: number;
  courseId: number;
  date: string;
  status: string;
  type: string;
}

export interface BulkAttendanceRequest {
  courseId: number;
  date: string;
  type: string;
  records: Array<{ studentId: number; status: string }>;
}

export interface AttendanceReport {
  studentId: number;
  studentName: string;
  totalClasses: number;
  present: number;
  absent: number;
  attendancePercentage: number;
}
