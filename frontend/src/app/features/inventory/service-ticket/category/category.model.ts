export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ServiceTicketCategory {
  id: number;
  name: string;
  description: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ServiceTicketCategoryRequest {
  name: string;
  description?: string;
  isActive?: boolean;
}

export interface ActiveStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface ActiveStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
