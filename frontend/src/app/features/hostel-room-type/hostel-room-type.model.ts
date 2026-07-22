export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface HostelRoomType {
  id: number;
  name: string;
  code: string;
  sharingCapacity: number;
  isAc: boolean;
  feeAmountPerYear: number;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface HostelRoomTypeRequest {
  name: string;
  code: string;
  sharingCapacity: number;
  isAc?: boolean;
  feeAmountPerYear: number;
  description?: string;
  isActive?: boolean;
}

export interface HostelRoomTypeStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface HostelRoomTypeStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
