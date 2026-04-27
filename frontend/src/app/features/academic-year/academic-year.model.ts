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
