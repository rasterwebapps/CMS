export type ClassSessionType = 'THEORY' | 'LAB' | 'CLINICAL';
export type CohortRoomAllocationStatus = 'COMMITTED' | 'REVERTED';

export interface VentureSplit {
  courseOfferingId: number;
  sessionType: 'LAB' | 'CLINICAL';
  venueId: number;
  batchName: string;
  plannedSize: number;
}

export interface CohortRoomAllocationCommitRequest {
  cohortId: number;
  termInstanceId: number;
  theoryClassroomId: number;
  ventureSplits: VentureSplit[];
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
}

export interface CohortRoomAllocation {
  id: number;
  cohortId: number;
  cohortLabel: string;
  termInstanceId: number;
  termLabel: string;
  status: CohortRoomAllocationStatus;
  theoryClassroomId: number;
  theoryClassroomName: string;
  theoryClassroomCapacity: number | null;
  committedBy: string | null;
  committedAt: string;
  revertedBy: string | null;
  revertedAt: string | null;
  batches: AllocatedBatch[];
}
