export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface Category {
  id: number;
  name: string;
  parentCategoryId: number | null;
  parentCategoryName: string | null;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryRequest {
  name: string;
  parentCategoryId?: number | null;
  description?: string;
  isActive?: boolean;
}

export interface CategoryStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface CategoryStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
