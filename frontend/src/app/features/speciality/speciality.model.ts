export interface Speciality {
  id: number;
  name: string;
  code: string;
  description?: string;
  hodFacultyId?: number;
  hodName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SpecialityRequest {
  name: string;
  code: string;
  description?: string;
  hodFacultyId?: number;
}
