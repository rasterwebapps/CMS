export interface Department {
  id: number;
  name: string;
  code: string;
  description?: string;
  /** FK to the faculty record designated as Head of Department. */
  hodFacultyId?: number;
  /** Denormalized display name, kept in sync by the backend service. */
  hodName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface DepartmentRequest {
  name: string;
  code: string;
  description?: string;
  /** ID of the faculty member to assign as Head of Department (optional). */
  hodFacultyId?: number;
}
