export interface EscortCandidate {
  facultyId: number;
  facultyName: string;
}

export interface EscortRotationMember {
  memberOrder: number;
  facultyId: number;
  facultyName: string;
}

export interface EscortRotationPool {
  batchId: number;
  batchName: string;
  rotationGroupId: number;
  cycleLength: number;
  anchorOccurrenceDate: string;
  members: EscortRotationMember[];
}

export interface EscortRotationPoolRequest {
  batchId: number;
  anchorOccurrenceDate: string;
  /** Ordered eligible-faculty pool — position fixes round-robin order. At least 2 required. */
  facultyIds: number[];
}

export interface EscortDuty {
  date: string;
  batchId: number;
  batchName: string;
  facultyId: number;
  facultyName: string;
  clinicalVenueName: string | null;
}
