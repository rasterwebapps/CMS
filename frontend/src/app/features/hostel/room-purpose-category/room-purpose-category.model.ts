export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface RoomPurposeCategory {
  id: number;
  name: string;
  code: string;
  isResidential: boolean;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface RoomPurposeCategoryRequest {
  name: string;
  code: string;
  isResidential?: boolean;
  description?: string;
  isActive?: boolean;
}

export interface RoomPurposeCategoryStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface RoomPurposeCategoryStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
