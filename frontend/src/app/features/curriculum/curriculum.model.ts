export interface Syllabus {
  id: number;
  courseId: number;
  courseName: string;
  courseCode: string;
  version: number;
  theoryHours?: number;
  labHours?: number;
  tutorialHours?: number;
  objectives?: string;
  content?: string;
  textBooks?: string;
  referenceBooks?: string;
  courseOutcomes?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SyllabusRequest {
  courseId: number;
  version: number;
  theoryHours?: number;
  labHours?: number;
  tutorialHours?: number;
  objectives?: string;
  content?: string;
  textBooks?: string;
  referenceBooks?: string;
  courseOutcomes?: string;
  isActive?: boolean;
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
