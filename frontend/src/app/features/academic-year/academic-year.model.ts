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

export interface Semester {
  id: number;
  name: string;
  semesterNumber: number;
  startDate: string;
  endDate: string;
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
