export type SkeletonSessionType = 'THEORY' | 'LAB' | 'CLINICAL';
export type SkeletonCellStatus = 'DRAFT' | 'PUBLISHED';

export interface SkeletonSubjectBudget {
  sessionType: SkeletonSessionType;
  batchId: number | null;
  batchName: string | null;
  /** Non-null only for a THEORY row once the cohort has a committed Cohort Room Allocation with
   *  one or more active sections — one budget row per section instead of a single whole-cohort
   *  row. Kept separate from batchId/batchName since they're semantically distinct occupants. */
  cohortSectionId: number | null;
  cohortSectionLabel: string | null;
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
  /** Non-null only for a grouped elective subject — every subject sharing the same group id must
   *  be placed in the same day/period this term (enforced server-side on placement). */
  electiveGroupId: number | null;
  electiveGroupName: string | null;
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
  /** Non-null only for a sectioned THEORY cell — which CohortSection it was placed for. */
  cohortSectionId: number | null;
  cohortSectionLabel: string | null;
  isStaffed: boolean;
  status: SkeletonCellStatus;
  /** Non-null only for a cell that's part of a Rotation Group — batchId/batchName are null on
   *  those (there's no single fixed occupant); rotatingBatchNames lists who alternates through it. */
  rotationGroupLabel: string | null;
  rotatingBatchNames: string[];
  courseOfferingId: number;
  subjectName: string;
  subjectCode: string;
  electiveGroupId: number | null;
  electiveGroupName: string | null;
}

export interface SkeletonBatchOption {
  id: number;
  courseOfferingId: number;
  name: string;
  capacity: number;
  enrolledCount: number;
}

/** One committed Theory room/section a THEORY placement can target — mirrors the backend's
 *  CohortSectionResponse (reused directly there rather than a duplicated shape). */
export interface SkeletonSectionOption {
  id: number;
  sectionLabel: string;
  classroomId: number;
  classroomName: string;
  classroomCapacity: number;
  plannedSize: number;
  isActive: boolean;
}

/** Cohort-wide since R3.1 — one response covers every non-elective subject a cohort has in a
 *  term, merging their cells/batches so cross-subject placement conflicts are visible in a
 *  single grid instead of hidden behind a per-subject filter. `sections` lists the cohort's
 *  active Cohort Room Allocation sections for this term (empty if none committed). */
export interface SkeletonBuilderResponse {
  cohortId: number;
  cohortName: string;
  termInstanceLabel: string;
  subjects: SkeletonSubject[];
  cells: SkeletonCell[];
  batches: SkeletonBatchOption[];
  sections: SkeletonSectionOption[];
}

export interface SkeletonCellPlacementRequest {
  courseOfferingId: number;
  sessionType: SkeletonSessionType;
  dayOfWeek: string;
  periodId: number;
  batchId: number | null;
  cohortId: number;
  /** THEORY only — required whenever the cohort has one or more active sections; null/ignored
   *  for LAB/CLINICAL (their section scope comes from the chosen batch instead). */
  cohortSectionId: number | null;
}

export interface SkeletonPlacementCandidate {
  dayOfWeek: string;
  periodId: number;
}

export interface SkeletonCellMoveRequest {
  dayOfWeek: string;
  periodId: number;
  cohortId: number;
}

export interface AutoPlaceUnplacedItem {
  subjectName: string;
  sessionType: SkeletonSessionType;
  occupantLabel: string | null;
  reason: string;
}

export interface AutoPlaceResult {
  placedCount: number;
  unplaced: AutoPlaceUnplacedItem[];
}

export interface ElectiveGroupMemberPlacement {
  courseOfferingId: number;
  sessionType: SkeletonSessionType;
  batchId: number | null;
  cohortSectionId: number | null;
}

export interface ElectiveGroupPlacementRequest {
  electiveGroupId: number;
  termInstanceId: number;
  cohortId: number;
  dayOfWeek: string;
  periodId: number;
  members: ElectiveGroupMemberPlacement[];
}

export interface ElectiveGroupScheduleResponse {
  scheduled: boolean;
  dayOfWeek: string | null;
  periodName: string | null;
  startTime: string | null;
  endTime: string | null;
}
