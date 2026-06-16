export type ProgramStatus = 'ACTIVE' | 'INACTIVE';
export type AssessmentPattern = 'TERM_BASED' | 'YEARLY';

export interface Program {
  id: number;
  name: string;
  code: string;
  durationYears: number;
  totalTerms: number;
  status: ProgramStatus;
  assessmentPattern: AssessmentPattern;
  mandatoryDocumentTypes: string[];
  optionalDocumentTypes: string[];
  minimumAgeYears: number;
  ageCutoffDay: number;
  ageCutoffMonth: number;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentRequirementsResponse {
  mandatory: string[];
  optional: string[];
}

export interface DocumentRequirementsRequest {
  mandatory: string[];
  optional: string[];
}

export interface ProgramRequest {
  name: string;
  code: string;
  durationYears: number;
  status?: ProgramStatus;
  assessmentPattern?: AssessmentPattern;
  minimumAgeYears: number;
  ageCutoffDay: number;
  ageCutoffMonth: number;
}

/**
 * Lightweight projection of a backend DocumentType, returned by GET /document-types.
 * Used as the single source of truth for rendering document type pickers and labels.
 */
export interface DocumentTypeInfo {
  code: string;
  label: string;
  category: string;
}
