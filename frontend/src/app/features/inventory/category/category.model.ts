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
  /** Prefix half of an auto-generated Product code, e.g. "STA" -> "STA-000001". Null for
   *  categories created before this feature, until an admin sets one. */
  shortCode: string | null;
  parentCategoryId: number | null;
  parentCategoryName: string | null;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryRequest {
  name: string;
  shortCode: string;
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
