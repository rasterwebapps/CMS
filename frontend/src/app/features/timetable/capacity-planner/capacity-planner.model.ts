export interface VenueOption {
  id: number;
  name: string;
  capacity: number | null;
}

export interface VenueUtilization {
  id: number;
  name: string;
  capacity: number | null;
  occupiedSlots: number;
  totalSlots: number;
  utilizationPercent: number;
}

export interface CapacityPlan {
  cohortId: number;
  cohortLabel: string;
  termInstanceId: number;
  termLabel: string;
  semesterNumber: number | null;
  cohortStrength: number;
  workingDaysInTerm: number;
  totalWorkingPeriodHours: number;
  blockedHours: number;
  curriculumHoursRequired: number;
  bufferHours: number;
  targetBatchSize: number;
  theoryFits: boolean;
  theoryShortfallMessage: string | null;
  fittingClassrooms: VenueOption[];
  labBatchesNeeded: number;
  fittingLabs: VenueOption[];
  clinicalBatchesNeeded: number;
  fittingClinicalVenues: VenueOption[];
  classroomUtilization: VenueUtilization[];
  labUtilization: VenueUtilization[];
  clinicalVenueUtilization: VenueUtilization[];
}
