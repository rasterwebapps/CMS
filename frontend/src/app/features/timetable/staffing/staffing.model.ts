export type StaffingSessionType = 'THEORY' | 'LAB' | 'CLINICAL';

export interface UnstaffedCell {
  id: number;
  courseOfferingId: number | null;
  subjectName: string;
  subjectCode: string;
  subjectSpecialityId: number | null;
  subjectSpecialityName: string | null;
  sessionType: StaffingSessionType;
  dayOfWeek: string;
  periodId: number;
  slotName: string;
  startTime: string;
  endTime: string;
  batchName: string | null;
  requiredStrength: number | null;
  /** LAB/CLINICAL rows, and non-elective THEORY rows, carry the venue already committed in
   *  Cohort Room Allocation (Capacity Planner). Null on one of those means it isn't committed yet
   *  and must be before it can be staffed. Elective THEORY rows always leave this null — they
   *  have no single owning cohort, so they keep a free classroom pick (see isElective). */
  venueId: number | null;
  venueName: string | null;
  venueCapacity: number | null;
  isElective: boolean;
  /** Non-empty only for a cell that's part of a Rotation Group — batchName is null on those
   *  (there's no single fixed occupant); this lists who alternates through it instead. */
  rotatingBatchNames: string[];
  /** The CohortSection this THEORY row was placed for, once its cohort's committed Theory room
   *  has been split into sections — null means "whole cohort" and, for LAB/CLINICAL, always null.
   *  Drives which eligible-faculty list (offering-level or section-level) backs the picker below,
   *  and which one's designated faculty (Course Offering Section Faculty override, if any) it
   *  pre-fills from. */
  cohortSectionId: number | null;
}

export interface StaffingAssignmentRequest {
  facultyId: number;
  /** Required only when the cell is an elective THEORY session — every other case resolves its
   *  room server-side from the committed Cohort Room Allocation venue. */
  classroomId: number | null;
}

export interface AutoStaffUnplacedItem {
  subjectName: string;
  reason: string;
}

export interface AutoStaffResult {
  staffedCount: number;
  unplaced: AutoStaffUnplacedItem[];
}
