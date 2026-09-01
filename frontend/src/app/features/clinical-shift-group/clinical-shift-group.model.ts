export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY';

export interface ClinicalShiftBatchLink {
  batchId: number;
  batchName: string;
  plannedSize: number;
  venueLabel: string | null;
}

export interface ClinicalShiftTheoryBlock {
  id: number;
  sequenceOrder: number;
  startTime: string;
  endTime: string;
  subjectId: number;
  subjectName: string;
  classroomId: number | null;
  classroomName: string | null;
}

export interface ClinicalShiftTheoryBlockRequest {
  sequenceOrder: number;
  startTime: string;
  endTime: string;
  subjectId: number;
  classroomId?: number | null;
}

export interface ClinicalShiftGroup {
  id: number;
  courseOfferingId: number;
  subjectName: string;
  cohortSectionId: number | null;
  cohortSectionLabel: string | null;
  termInstanceId: number;
  label: string;
  dayOfWeek: DayOfWeek;
  clinicalStartTime: string;
  clinicalEndTime: string | null;
  busDepartTime: string | null;
  busReturnTime: string | null;
  isActive: boolean;
  batches: ClinicalShiftBatchLink[];
  theoryBlocks: ClinicalShiftTheoryBlock[];
  createdAt: string;
  updatedAt: string;
}

export interface ClinicalShiftGroupRequest {
  courseOfferingId: number;
  cohortSectionId?: number | null;
  label: string;
  dayOfWeek: DayOfWeek;
  clinicalStartTime: string;
}

export interface ClinicalShiftConfigUpdateRequest {
  clinicalShiftDurationMinutes: number | null;
  clinicalTravelBufferMinutes: number | null;
}
