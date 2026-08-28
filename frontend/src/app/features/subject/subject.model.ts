import { Speciality } from '../speciality/speciality.model';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

/** Minimal venue shape used for a Subject's eligible Labs/Clinical Venues — same fields as the
 *  backend's shared VenueOptionResponse. */
export interface SubjectEligibleVenue {
  id: number;
  name: string;
  capacity: number | null;
}

/** Faculty explicitly widened onto this subject on top of the Speciality-match rule — additive
 *  only, see backend FacultyEligibility. Empty means Speciality-match-only. */
export interface SubjectEligibleFaculty {
  id: number;
  fullName: string;
  specialityName: string | null;
}

export interface Subject {
  id: number;
  name: string;
  code: string;
  credits: number;
  theoryCredits: number;
  labCredits: number;
  speciality: Speciality | null;
  termNumber: number;
  isActive: boolean;
  /** How many consecutive periods one single Lab/Clinical session must occupy for this subject
   *  (e.g. a 3-hour lab runs as 3 back-to-back periods on the same day, not scattered
   *  single-period placements). Default 1 = independent single-period placements. */
  labSessionBlockPeriods: number;
  clinicalSessionBlockPeriods: number;
  createdAt: string;
  updatedAt: string;
  /** Labs/Clinical Venues suitable for this subject's practical sessions — a soft preference for
   *  the auto-suggest algorithm and manual pickers, not a hard restriction. Empty means no
   *  preference configured. */
  eligibleLabs: SubjectEligibleVenue[];
  eligibleClinicalVenues: SubjectEligibleVenue[];
  eligibleFaculty: SubjectEligibleFaculty[];
}

export interface SubjectRequest {
  name: string;
  code: string;
  credits: number;
  theoryCredits: number;
  labCredits: number;
  specialityId: number | null;
  termNumber: number;
  isActive?: boolean;
  labSessionBlockPeriods?: number;
  clinicalSessionBlockPeriods?: number;
  eligibleLabIds?: number[];
  eligibleClinicalVenueIds?: number[];
  eligibleFacultyIds?: number[];
}

export interface SubjectStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface SubjectStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
