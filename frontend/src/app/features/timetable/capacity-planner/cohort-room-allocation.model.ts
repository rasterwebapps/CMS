import { PlanningBasis } from './capacity-planner.model';

export type ClassSessionType = 'THEORY' | 'LAB' | 'CLINICAL';
export type CohortRoomAllocationStatus = 'COMMITTED' | 'REVERTED';

export interface CohortSectionRequest {
  sectionLabel: string;
  classroomId: number;
  plannedSize: number;
}

export interface VentureSplit {
  courseOfferingId: number;
  sessionType: 'LAB' | 'CLINICAL';
  venueId: number;
  batchName: string;
  plannedSize: number;
  /** Correlates to a CohortSectionRequest.sectionLabel in the same commit request -- sections
   *  don't have real database ids yet when this request is built. Optional when the commit has
   *  exactly one section (auto-resolves), required when it has 2+. */
  cohortSectionLabel: string | null;
}

export interface CohortRoomAllocationCommitRequest {
  cohortId: number;
  termInstanceId: number;
  planningBasis: PlanningBasis;
  sections: CohortSectionRequest[];
  ventureSplits: VentureSplit[];
}

export interface CohortSection {
  id: number;
  sectionLabel: string;
  classroomId: number;
  classroomName: string;
  classroomCapacity: number | null;
  plannedSize: number;
  isActive: boolean;
}

export interface AllocatedBatch {
  batchId: number;
  courseOfferingId: number;
  subjectName: string;
  sessionType: ClassSessionType;
  venueId: number | null;
  venueName: string | null;
  venueCapacity: number | null;
  batchName: string;
  plannedSize: number;
  isActive: boolean;
  cohortSectionId: number | null;
  cohortSectionLabel: string | null;
}

export interface CohortRoomAllocation {
  id: number;
  cohortId: number;
  cohortLabel: string;
  termInstanceId: number;
  termLabel: string;
  status: CohortRoomAllocationStatus;
  planningBasis: PlanningBasis;
  plannedStrength: number;
  sections: CohortSection[];
  committedBy: string | null;
  committedAt: string;
  revertedBy: string | null;
  revertedAt: string | null;
  batches: AllocatedBatch[];
}
