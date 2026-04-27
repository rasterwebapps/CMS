export interface CurriculumVersion {
  id: number;
  programId: number;
  programName: string;
  versionName: string;
  effectiveFromAcademicYearId: number;
  effectiveFromAcademicYearName: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CurriculumVersionRequest {
  programId: number;
  versionName: string;
  effectiveFromAcademicYearId: number;
  isActive?: boolean;
}

export interface CurriculumSemesterCourse {
  id: number;
  curriculumVersionId: number;
  curriculumVersionName: string;
  semesterNumber: number;
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  sortOrder?: number;
  createdAt: string;
  updatedAt: string;
}

export interface CurriculumSemesterCourseRequest {
  curriculumVersionId: number;
  semesterNumber: number;
  subjectId: number;
  sortOrder?: number;
}

export interface CurriculumSemesterGroup {
  semesterNumber: number;
  courses: CurriculumSemesterCourse[];
}

export interface CurriculumFullView {
  curriculumVersionId: number;
  versionName: string;
  programId: number;
  programName: string;
  totalSemesters: number;
  semesters: CurriculumSemesterGroup[];
}
