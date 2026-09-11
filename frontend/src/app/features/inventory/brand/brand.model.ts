export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface Brand {
  id: number;
  name: string;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface BrandRequest {
  name: string;
  description?: string;
  isActive?: boolean;
}

export interface BrandStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface BrandStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
