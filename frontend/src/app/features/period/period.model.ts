export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface Period {
  id: number;
  name: string;
  startTime: string;
  endTime: string;
  periodOrder?: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PeriodRequest {
  name: string;
  startTime: string;
  endTime: string;
  periodOrder?: number;
  isActive?: boolean;
}

export interface PeriodStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface PeriodStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
