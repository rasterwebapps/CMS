export type PlanningBasis = 'ENROLLED' | 'SANCTIONED';

/** One faculty's row in the advisory, term-wide Faculty Workload capacity report — see
 *  `FacultyWorkloadCapacityService` on the backend for how each figure is computed. Purely a
 *  dashboard: never blocks anything, never auto-allocates. */
export interface FacultyWorkloadRow {
  facultyId: number;
  facultyName: string;
  designationName: string | null;
  demandHoursPerWeek: number;
  committedHoursPerWeek: number;
  blockedHoursPerWeek: number;
  capacityConfigured: boolean;
  effectiveCapacityHours: number | null;
  netCapacityHours: number | null;
  overDemand: boolean;
  overCommitted: boolean;
}

export interface FacultyWorkloadReport {
  termInstanceId: number;
  rows: FacultyWorkloadRow[];
  totalDemandHoursPerWeek: number;
  totalCommittedHoursPerWeek: number;
  totalConfiguredCapacityHoursPerWeek: number;
  unconfiguredFacultyCount: number;
}

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

/** One auto-suggested Theory section — same shape a human would produce via the manual draft
 *  builder, computed server-side by the fewest-rooms greedy fill (see
 *  TimetableCapacityPlanningService.suggestSections). */
export interface SuggestedSection {
  sectionLabel: string;
  classroomId: number;
  classroomName: string;
  classroomCapacity: number | null;
  plannedSize: number;
}

/** One auto-suggested Lab/Clinical batch row for a SuggestedSection. batchLabel is null when the
 *  section's whole headcount fits the suggested venue unsplit, "Batch 1"/"Batch 2"... when it was
 *  greedily packed into multiple venue-sized batches. */
export interface SuggestedBatch {
  courseOfferingId: number;
  subjectName: string;
  sessionType: 'LAB' | 'CLINICAL';
  venueId: number;
  venueName: string;
  venueCapacity: number | null;
  sectionLabel: string;
  batchLabel: string | null;
  plannedSize: number;
  /** The subject's own configured-eligible venue IDs for this session type (Subject.eligibleLabs/
   *  eligibleClinicalVenues) — always present even when venueId itself came from the full-pool
   *  fallback. Empty when the subject has no eligible venues configured. Used to sort/highlight the
   *  subject's real preference in manual venue pickers without a second lookup. */
  eligibleVenueIds: number[];
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
  suggestedSections: SuggestedSection[];
  suggestedLabClinicalBatches: SuggestedBatch[];
  /** Whether every Lab/Clinical-hour subject in this plan has a designated venue mapping
   *  (Subject.eligibleLabs/eligibleClinicalVenues) configured with enough combined capacity --
   *  auto-suggestion is designated-only (never substitutes an unrelated venue), so false here means
   *  a real data gap, not just a missed optimization. */
  labClinicalMappingSufficient: boolean;
  labClinicalMappingIssuesMessage: string | null;
}

/** One row of the term-wide Capacity Auto-Plan overview screen — one per Cohort enrolled in a
 *  TermInstance. Committed cohorts are never re-planned; suggestedSections/
 *  suggestedLabClinicalBatches are empty for them. */
export interface CohortAutoPlanSummary {
  cohortId: number;
  cohortLabel: string;
  semesterNumber: number | null;
  cohortStrength: number;
  hasCommittedAllocation: boolean;
  theoryFits: boolean;
  theoryShortfallMessage: string | null;
  suggestedSections: SuggestedSection[];
  suggestedLabClinicalBatches: SuggestedBatch[];
  labClinicalMappingSufficient: boolean;
  labClinicalMappingIssuesMessage: string | null;
}

/** One physical room in the Capacity Auto-Plan overview's whole-term room inventory. A CLASSROOM
 *  is full-or-empty (claimedByCohortLabel only, matching Theory's exclusive-per-term lock — no
 *  percentage, occupiedSlots/totalSlots/utilizationPercent are 0 and unused). Lab/Clinical venues
 *  are always shareable, so claimedByCohortLabel is always null for those, and they instead carry
 *  real weekly period-slot occupancy — utilizationPercent can exceed 100 when a venue has genuine
 *  Saturday bookings beyond the 5-day routine-week baseline, which is intentional, not a bug.
 *  suggestedBookingCount is informational only — how many not-yet-committed cohorts' suggestions
 *  reference this room this pass. */
export interface RoomInventoryRow {
  id: number;
  name: string;
  roomType: 'CLASSROOM' | 'LAB' | 'CLINICAL';
  capacity: number | null;
  claimedByCohortLabel: string | null;
  suggestedBookingCount: number;
  occupiedSlots: number;
  totalSlots: number;
  utilizationPercent: number;
}

/** Whole-term response backing the Capacity Auto-Plan screen. theorySufficient is a strict
 *  pass/fail (free classroom capacity vs. summed not-yet-planned cohort headcount) — there is
 *  deliberately no Lab/Clinical equivalent for DAY/PERIOD-COLLISION sufficiency, since those venues
 *  are shared across cohorts at different times and this stage has no day/period data to know if two
 *  suggestions would collide. labClinicalMappingSufficient is a different, narrower check: whether
 *  every not-yet-planned cohort's Lab/Clinical subjects have a designated venue mapping configured
 *  with enough capacity — unrelated to timing collisions. */
export interface TermCapacityOverview {
  termInstanceId: number;
  theorySufficient: boolean;
  totalFreeClassroomCapacity: number;
  totalNotPlannedStrength: number;
  theorySufficiencyMessage: string | null;
  cohorts: CohortAutoPlanSummary[];
  roomInventory: RoomInventoryRow[];
  labClinicalMappingSufficient: boolean;
  labClinicalMappingIssuesMessage: string | null;
}
