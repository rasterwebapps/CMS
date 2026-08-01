export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ClinicalVenue {
  id: number;
  name: string;
  hospitalName?: string;
  department?: string;
  capacity?: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ClinicalVenueRequest {
  name: string;
  hospitalName?: string;
  department?: string;
  capacity?: number;
  isActive?: boolean;
}

export interface ClinicalVenueStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface ClinicalVenueStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
