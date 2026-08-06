export interface RotationCandidateSlot {
  classScheduleId: number;
  courseOfferingId: number | null;
  subjectName: string;
  batchId: number | null;
  batchName: string | null;
  sessionType: 'THEORY' | 'LAB' | 'CLINICAL';
  dayOfWeek: string;
  periodId: number;
  periodName: string | null;
}

export interface RotationSlotInput {
  classScheduleId: number;
  slotOrder: number;
}

export interface RotationAssignmentInput {
  classScheduleId: number;
  batchId: number;
}

export interface RotationMemberInput {
  memberOrder: number;
  label: string;
  assignments: RotationAssignmentInput[];
}

export interface RotationGroupCreateRequest {
  termInstanceId: number;
  label: string;
  anchorOccurrenceDate: string;
  slots: RotationSlotInput[];
  members: RotationMemberInput[];
}

export interface RotationSlotResponse {
  id: number;
  classScheduleId: number;
  slotOrder: number;
  subjectName: string;
  sessionType: string;
  dayOfWeek: string;
  periodName: string | null;
}

export interface RotationAssignmentResponse {
  rotationSlotId: number;
  classScheduleId: number;
  batchId: number;
  batchName: string;
}

export interface RotationMemberResponse {
  id: number;
  memberOrder: number;
  label: string;
  assignments: RotationAssignmentResponse[];
}

export interface RotationGroupResponse {
  id: number;
  termInstanceId: number;
  label: string;
  cycleLength: number;
  anchorOccurrenceDate: string;
  slots: RotationSlotResponse[];
  members: RotationMemberResponse[];
  warnings: string[];
}
