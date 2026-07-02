export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface Community {
  id: number;
  name: string;
  code: string;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CommunityRequest {
  name: string;
  code: string;
  description?: string;
  isActive?: boolean;
}

export interface CommunityStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface CommunityStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}

