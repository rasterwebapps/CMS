export interface GuardianRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string | null;
  relationshipHint?: string | null;
}

export interface GuardianResponse {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  relationshipHint: string | null;
  createdAt: string;
}

/** A guardian as seen from one specific student's Guardians tab -- carries the link's own
 *  isPrimary flag, unlike the plain admin-wide GuardianResponse. */
export interface StudentGuardianResponse extends GuardianResponse {
  isPrimary: boolean;
}

/** A lightweight ward summary powering the ward switcher -- never the full Student record. */
export interface WardSummaryResponse {
  studentId: number;
  fullName: string;
  rollNumber: string;
  isPrimary: boolean;
}
