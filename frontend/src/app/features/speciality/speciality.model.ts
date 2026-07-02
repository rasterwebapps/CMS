export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface Speciality {
  id: number;
  name: string;
  code: string;
  description?: string;
  hodFacultyId?: number;
  hodName?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SpecialityRequest {
  name: string;
  code: string;
  description?: string;
  hodFacultyId?: number;
  isActive?: boolean;
}

export interface SpecialityStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface SpecialityStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
