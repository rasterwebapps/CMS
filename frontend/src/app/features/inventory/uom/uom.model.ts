export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface Uom {
  id: number;
  code: string;
  name: string;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UomRequest {
  code: string;
  name: string;
  description?: string;
  isActive?: boolean;
}

export interface UomStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface UomStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
