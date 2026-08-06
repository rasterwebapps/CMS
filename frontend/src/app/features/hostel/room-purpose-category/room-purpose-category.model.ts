export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

/** Fixed, backend-enforced identity set (RoomPurposeCategoryCode) — mirrors the DB CHECK
 *  constraint added in V362. This list only grows via a backend enum value + migration together,
 *  never by typing an arbitrary string, so nothing that keys off e.g. 'ACADEMIC' can be silently
 *  broken by a rename. */
export type RoomPurposeCategoryCode =
  | 'ACADEMIC'
  | 'RESIDENTIAL'
  | 'ADMIN_STAFF'
  | 'LIBRARY'
  | 'DINING'
  | 'UTILITY'
  | 'SPORTS';

export const ROOM_PURPOSE_CATEGORY_CODE_LABELS: Record<RoomPurposeCategoryCode, string> = {
  ACADEMIC: 'Academic',
  RESIDENTIAL: 'Residential (Hostel)',
  ADMIN_STAFF: 'Administrative & Staff',
  LIBRARY: 'Knowledge & Resource',
  DINING: 'Dining & Refreshment',
  UTILITY: 'Hygiene & Utility',
  SPORTS: 'Sports & Recreation',
};

export interface RoomPurposeCategory {
  id: number;
  name: string;
  code: RoomPurposeCategoryCode;
  isResidential: boolean;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface RoomPurposeCategoryRequest {
  name: string;
  /** Ignored by the backend on update — immutable once set. Only meaningful on create. */
  code: RoomPurposeCategoryCode;
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
