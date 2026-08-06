export type PlanningBasis = 'ENROLLED' | 'SANCTIONED';

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
  /** Non-null only for a Classroom with a genuine active claim this term from another cohort --
   *  Labs/Clinical Venues are never exclusively claimed per-term, so always null for those. */
  claimedByCohortLabel: string | null;
}

export interface CapacityPlan {
  cohortId: number;
  cohortLabel: string;
  termInstanceId: number;
  termLabel: string;
  semesterNumber: number | null;
  cohortStrength: number;
  enrolledStrength: number;
  sanctionedStrength: number | null;
  workingDaysInTerm: number;
  totalWorkingPeriodHours: number;
  blockedHours: number;
  curriculumHoursRequired: number;
  bufferHours: number;
  theoryFits: boolean;
  theoryShortfallMessage: string | null;
  fittingClassrooms: VenueOption[];
  classroomsForSectioning: VenueOption[];
  fittingLabs: VenueOption[];
  fittingClinicalVenues: VenueOption[];
  classroomUtilization: VenueUtilization[];
  labUtilization: VenueUtilization[];
  clinicalVenueUtilization: VenueUtilization[];
}
