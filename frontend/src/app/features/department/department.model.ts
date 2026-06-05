export interface Speciality {
  id: number;
  name: string;
  code: string;
  description?: string;
  /** FK to the faculty record designated as Head of Speciality. */
  hodFacultyId?: number;
  /** Denormalized display name, kept in sync by the backend service. */
  hodName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SpecialityRequest {
  name: string;
  code: string;
  description?: string;
  /** ID of the faculty member to assign as Head of Speciality (optional). */
  hodFacultyId?: number;
}
