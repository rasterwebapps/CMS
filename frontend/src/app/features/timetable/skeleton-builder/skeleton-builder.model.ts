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

export interface SkeletonSubject {
  courseOfferingId: number;
  subjectName: string;
  subjectCode: string;
  budgets: SkeletonSubjectBudget[];
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
  courseOfferingId: number;
  subjectName: string;
  subjectCode: string;
}

export interface SkeletonBatchOption {
  id: number;
  courseOfferingId: number;
  name: string;
  capacity: number;
  enrolledCount: number;
}

/** Cohort-wide since R3.1 — one response covers every non-elective subject a cohort has in a
 *  term, merging their cells/batches so cross-subject placement conflicts are visible in a
 *  single grid instead of hidden behind a per-subject filter. */
export interface SkeletonBuilderResponse {
  cohortId: number;
  cohortName: string;
  termInstanceLabel: string;
  subjects: SkeletonSubject[];
  cells: SkeletonCell[];
  batches: SkeletonBatchOption[];
}

export interface SkeletonCellPlacementRequest {
  courseOfferingId: number;
  sessionType: SkeletonSessionType;
  dayOfWeek: string;
  periodId: number;
  batchId: number | null;
  cohortId: number;
}

export interface SkeletonPlacementCandidate {
  dayOfWeek: string;
  periodId: number;
}
