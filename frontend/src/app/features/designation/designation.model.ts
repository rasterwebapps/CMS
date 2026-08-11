export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface DesignationMaster {
  id: number;
  name: string;
  code: string;
  description?: string;
  isActive: boolean;
  /** Advisory-only default for the faculty capacity-planning report; a per-faculty override wins
   *  over this when set. Undefined means unconfigured, not zero. */
  defaultWeeklyTeachingHours?: number;
  createdAt: string;
  updatedAt: string;
}

export interface DesignationRequest {
  name: string;
  code: string;
  description?: string;
  isActive?: boolean;
  defaultWeeklyTeachingHours?: number;
}

export interface DesignationStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface DesignationStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
