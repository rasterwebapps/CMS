export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface RoomSubType {
  id: number;
  purposeCategoryId: number;
  purposeCategoryName: string;
  name: string;
  code: string;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface RoomSubTypeRequest {
  purposeCategoryId: number;
  name: string;
  code: string;
  description?: string;
  isActive?: boolean;
}

export interface RoomSubTypeStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface RoomSubTypeStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
