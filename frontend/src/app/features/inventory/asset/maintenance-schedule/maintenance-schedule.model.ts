export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type MaintenanceScheduleType = 'ONE_OFF' | 'RECURRING';

export interface AssetMaintenanceScheduleRequest {
  assetId: number;
  scheduleType: MaintenanceScheduleType;
  recurrenceIntervalDays?: number;
  nextDueDate: string;
  notes?: string;
}

export interface AssetMaintenanceMarkPerformedRequest {
  performedDate?: string;
  notes?: string;
}

export interface AssetMaintenanceSchedule {
  id: number;
  assetId: number;
  assetTag: string;
  productName: string;
  scheduleType: MaintenanceScheduleType;
  recurrenceIntervalDays: number | null;
  nextDueDate: string;
  lastPerformedDate: string | null;
  overdue: boolean;
  isActive: boolean;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}
