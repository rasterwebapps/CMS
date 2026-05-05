export type ProgramStatus = 'ACTIVE' | 'INACTIVE';

export interface Program {
  id: number;
  name: string;
  code: string;
  durationYears: number;
  totalSemesters: number;
  status: ProgramStatus;
  requiredDocumentTypes: string[];
  createdAt: string;
  updatedAt: string;
}

export interface ProgramRequest {
  name: string;
  code: string;
  durationYears: number;
  status?: ProgramStatus;
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
