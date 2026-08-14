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
  /** Same override precedence as {@link defaultWeeklyTeachingHours}, feeds the daily hard cap. */
  defaultDailyTeachingHours?: number;
  /** Same override precedence as {@link defaultWeeklyTeachingHours}, feeds the continuous hard cap. */
  defaultContinuousTeachingHours?: number;
  createdAt: string;
  updatedAt: string;
}

export interface DesignationRequest {
  name: string;
  code: string;
  description?: string;
  isActive?: boolean;
  defaultWeeklyTeachingHours?: number;
  defaultDailyTeachingHours?: number;
  defaultContinuousTeachingHours?: number;
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
