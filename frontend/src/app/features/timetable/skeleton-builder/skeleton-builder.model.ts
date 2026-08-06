export type SkeletonSessionType = 'THEORY' | 'LAB' | 'CLINICAL';
export type SkeletonCellStatus = 'DRAFT' | 'PUBLISHED';

export interface SkeletonSubjectBudget {
  sessionType: SkeletonSessionType;
  batchId: number | null;
  batchName: string | null;
  totalHours: number;
  weeksInTerm: number;
  requiredSessionsPerWeek: number;
  placedSessionsPerWeek: number;
}

export interface SkeletonCell {
  id: number;
  sessionType: SkeletonSessionType;
  dayOfWeek: string;
  periodId: number;
  slotName: string;
  startTime: string;
  endTime: string;
  batchId: number | null;
  batchName: string | null;
  isStaffed: boolean;
  status: SkeletonCellStatus;
  /** Non-null only for a cell that's part of a Rotation Group — batchId/batchName are null on
   *  those (there's no single fixed occupant); rotatingBatchNames lists who alternates through it. */
  rotationGroupLabel: string | null;
  rotatingBatchNames: string[];
}

export interface SkeletonBatchOption {
  id: number;
  name: string;
  capacity: number;
  enrolledCount: number;
}

export interface SkeletonBuilderResponse {
  courseOfferingId: number;
  subjectName: string;
  subjectCode: string;
  termInstanceLabel: string;
  budgets: SkeletonSubjectBudget[];
  cells: SkeletonCell[];
  batches: SkeletonBatchOption[];
}

export interface SkeletonCellPlacementRequest {
  courseOfferingId: number;
  sessionType: SkeletonSessionType;
  dayOfWeek: string;
  periodId: number;
  batchId: number | null;
}
