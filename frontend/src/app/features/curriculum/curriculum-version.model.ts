export interface CurriculumVersion {
  id: number;
  programId: number;
  programName: string;
  /** Optional narrower scope than program — set when this version applies to one specific
   *  course only (e.g. MSc Nursing (Adult) vs (Child), which share a Program). */
  courseId: number | null;
  courseName: string | null;
  versionName: string;
  effectiveFromAcademicYearId: number;
  effectiveFromAcademicYearName: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CurriculumVersionRequest {
  programId: number;
  /** Optional — leave undefined/null for a program-wide version. */
  courseId?: number | null;
  versionName: string;
  effectiveFromAcademicYearId: number;
  isActive?: boolean;
}

export type SubjectType = 'CORE' | 'FOUNDATIONAL' | 'ELECTIVE';

export interface CurriculumSemesterCourse {
  id: number;
  curriculumVersionId: number;
  curriculumVersionName: string;
  termNumber: number;
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  sortOrder?: number;
  theoryHours: number;
  labHours: number;
  clinicalHours: number;
  subjectType: SubjectType;
  isElective: boolean;
  electiveGroupId: number | null;
  electiveGroupName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CurriculumSemesterCourseRequest {
  curriculumVersionId: number;
  termNumber: number;
  subjectId: number;
  sortOrder?: number;
  theoryHours?: number;
  labHours?: number;
  clinicalHours?: number;
  subjectType?: SubjectType;
  isElective?: boolean;
  electiveGroupId?: number | null;
}

export interface CurriculumElectiveGroup {
  id: number;
  curriculumVersionId: number;
  termNumber: number;
  groupName: string;
  groupCode: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CurriculumElectiveGroupRequest {
  curriculumVersionId: number;
  termNumber: number;
  groupName: string;
  groupCode?: string;
}

export type AttendanceComponentType = 'THEORY' | 'LAB' | 'CLINICAL';

export interface AttendanceThreshold {
  id: number;
  curriculumTermCourseId: number;
  attendanceType: AttendanceComponentType;
  minPercentage: number;
  createdAt: string;
  updatedAt: string;
}

export interface AttendanceThresholdRequest {
  curriculumTermCourseId: number;
  attendanceType: AttendanceComponentType;
  minPercentage: number;
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
