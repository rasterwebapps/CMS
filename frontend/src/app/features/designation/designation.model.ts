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
  defaultWeeklyTeachingSessions?: number;
  /** Same override precedence as {@link defaultWeeklyTeachingSessions}, feeds the daily hard cap. */
  defaultDailyTeachingSessions?: number;
  /** Same override precedence as {@link defaultWeeklyTeachingSessions}, feeds the continuous hard cap. */
  defaultContinuousTeachingSessions?: number;
  createdAt: string;
  updatedAt: string;
}

export interface DesignationRequest {
  name: string;
  code: string;
  description?: string;
  isActive?: boolean;
  defaultWeeklyTeachingSessions?: number;
  defaultDailyTeachingSessions?: number;
  defaultContinuousTeachingSessions?: number;
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
