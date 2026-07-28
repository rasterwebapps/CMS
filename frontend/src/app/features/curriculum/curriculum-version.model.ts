export interface CurriculumVersion {
  id: number;
  programId: number;
  programName: string;
  /** The specific course under the program this version applies to (e.g. MSc Nursing
   *  (Adult) vs (Child), which share a Program). Mandatory. */
  courseId: number;
  courseName: string;
  versionName: string;
  effectiveFromAcademicYearId: number;
  effectiveFromAcademicYearName: string;
  isActive: boolean;
  termCount: number;
  subjectCount: number;
  /** False when subjects are mapped into this version or course offerings reference it — blocks delete. */
  deletable: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface CurriculumVersionRequest {
  programId: number;
  courseId: number;
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
  /** True once a course offering has been generated against this mapping — hours, subject
   *  type, and elective status become read-only at that point to avoid retroactively
   *  invalidating attendance thresholds/registrations already recorded against it. */
  isLocked: boolean;
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

export interface SyllabusUnit {
  id: number;
  curriculumTermCourseId: number;
  unitNumber: number;
  title: string;
  componentType: AttendanceComponentType;
  plannedHours: number | null;
  description: string | null;
  sortOrder: number | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SyllabusUnitRequest {
  curriculumTermCourseId: number;
  unitNumber: number;
  title: string;
  componentType: AttendanceComponentType;
  plannedHours?: number | null;
  description?: string | null;
  sortOrder?: number | null;
}

export interface CurriculumSemesterGroup {
  termNumber: number;
  courses: CurriculumSemesterCourse[];
}

export interface CurriculumFullView {
  curriculumVersionId: number;
  curriculumVersionName: string;
  programId: number;
  programName: string;
  assessmentPattern: 'TERM_BASED' | 'YEARLY';
  totalTerms: number;
  terms: CurriculumSemesterGroup[];
}
