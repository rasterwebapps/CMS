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
  termNumber: number;
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  sortOrder?: number;
  createdAt: string;
  updatedAt: string;
}

export interface CurriculumSemesterCourseRequest {
  curriculumVersionId: number;
  termNumber: number;
  subjectId: number;
  sortOrder?: number;
}

export interface CurriculumSemesterGroup {
  termNumber: number;
  courses: CurriculumSemesterCourse[];
}

export interface CurriculumFullView {
  curriculumVersionId: number;
  versionName: string;
  programId: number;
  programName: string;
  assessmentPattern: 'TERM_BASED' | 'YEARLY';
  totalTerms: number;
  terms: CurriculumSemesterGroup[];
}
