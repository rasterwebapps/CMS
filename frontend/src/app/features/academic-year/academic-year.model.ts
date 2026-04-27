export interface AcademicYear {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  isCurrent: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AcademicYearRequest {
  name: string;
  startDate: string;
  endDate: string;
  isCurrent: boolean;
}

export type SemesterStatus = 'UPCOMING' | 'ONGOING' | 'COMPLETED';

export interface Semester {
  id: number;
  name: string;
  semesterNumber: number;
  startDate: string;
  endDate: string;
  status: SemesterStatus;
  academicYear: AcademicYear;
  createdAt: string;
  updatedAt: string;
}

export interface SemesterRequest {
  name: string;
  semesterNumber: number;
  startDate: string;
  endDate: string;
  academicYearId: number;
}

export type CalendarEventType = 'HOLIDAY' | 'EXAM' | 'CULTURAL' | 'SPORTS' | 'WORKSHOP' | 'OTHER';

export interface CalendarEvent {
  id: number;
  title: string;
  description?: string;
  startDate: string;
  endDate: string;
  eventType: CalendarEventType;
  academicYear: AcademicYear;
  semester?: Semester;
  createdAt: string;
  updatedAt: string;
}

export interface CalendarEventRequest {
  title: string;
  description?: string;
  startDate: string;
  endDate: string;
  eventType: CalendarEventType;
  academicYearId: number;
  semesterId?: number;
}

export type TermType = 'ODD' | 'EVEN';
export type TermInstanceStatus = 'PLANNED' | 'OPEN' | 'LOCKED';
export type LateFeeType = 'FLAT' | 'PER_DAY';

export interface TermInstance {
  id: number;
  academicYearId: number;
  academicYearName: string;
  termType: TermType;
  startDate: string;
  endDate: string;
  status: TermInstanceStatus;
  createdAt: string;
  updatedAt: string;
}

export interface TermInstanceUpdateRequest {
  startDate?: string | null;
  endDate?: string | null;
  status?: TermInstanceStatus | null;
}

export interface TermBillingSchedule {
  id: number;
  academicYearId: number;
  academicYearName: string;
  termType: TermType;
  dueDate: string;
  lateFeeType: LateFeeType;
  lateFeeAmount: number;
  graceDays: number;
  createdAt: string;
  updatedAt: string;
}

export interface TermBillingScheduleRequest {
  academicYearId: number;
  termType: TermType;
  dueDate: string;
  lateFeeType: LateFeeType;
  lateFeeAmount: number;
  graceDays: number;
}

export type EnrollmentStatus = 'ENROLLED' | 'COMPLETED' | 'DROPPED';

export interface StudentTermEnrollment {
  id: number;
  studentId: number;
  studentName: string;
  cohortId: number;
  cohortCode: string;
  termInstanceId: number;
  termInstanceLabel: string;
  semesterNumber: number;
  yearOfStudy: number;
  status: EnrollmentStatus;
}

export interface GenerateEnrollmentsResponse {
  enrollmentsCreated: number;
}
