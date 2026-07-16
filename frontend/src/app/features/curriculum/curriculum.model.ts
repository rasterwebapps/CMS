export interface Syllabus {
  id: number;
  curriculumTermCourseId: number;
  curriculumVersionId: number;
  curriculumVersionName: string;
  termNumber: number;
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  version: number;
  /** Read-only — Curriculum Map is the source of truth for these; derived from the linked mapping. */
  theoryHours?: number;
  labHours?: number;
  clinicalHours?: number;
  objectives?: string;
  content?: string;
  textBooks?: string;
  referenceBooks?: string;
  courseOutcomes?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

/** Create-only — a syllabus is immutable once created (see BR: syllabus versioning).
 *  Version is auto-assigned by the backend (next integer for the mapping). */
export interface SyllabusRequest {
  curriculumTermCourseId: number;
  objectives?: string;
  content?: string;
  textBooks?: string;
  referenceBooks?: string;
  courseOutcomes?: string;
  isActive?: boolean;
}

/** The only permitted change to an existing syllabus version. */
export interface SyllabusActivationRequest {
  isActive: boolean;
}

export interface Experiment {
  id: number;
  courseId: number;
  courseName: string;
  courseCode: string;
  experimentNumber: number;
  name: string;
  description?: string;
  aim?: string;
  apparatus?: string;
  procedure?: string;
  expectedOutcome?: string;
  learningOutcomes?: string;
  estimatedDurationMinutes?: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ExperimentRequest {
  courseId: number;
  experimentNumber: number;
  name: string;
  description?: string;
  aim?: string;
  apparatus?: string;
  procedure?: string;
  expectedOutcome?: string;
  learningOutcomes?: string;
  estimatedDurationMinutes?: number;
  isActive?: boolean;
}

export interface LabCurriculumMapping {
  id: number;
  experimentId: number;
  experimentName: string;
  experimentNumber: number;
  courseId: number;
  courseName: string;
  outcomeType: string;
  outcomeCode: string;
  outcomeDescription?: string;
  mappingLevel: string;
  justification?: string;
  createdAt: string;
  updatedAt: string;
}

export interface LabCurriculumMappingRequest {
  experimentId: number;
  outcomeType: string;
  outcomeCode: string;
  outcomeDescription?: string;
  mappingLevel: string;
  justification?: string;
}
