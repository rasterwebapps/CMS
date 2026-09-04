export interface Batch {
  id: number;
  courseOfferingId: number;
  name: string;
  capacity: number;
  enrolledCount: number;
  termInstanceId: number;
  coordinatorFacultyId: number | null;
  coordinatorFacultyName: string | null;
  labId: number | null;
  labName: string | null;
  clinicalVenueId: number | null;
  clinicalVenueName: string | null;
  clinicalShiftGroupId: number | null;
  cohortSectionId: number | null;
  sectionLabel: string | null;
  isActive: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface BatchRequest {
  courseOfferingId: number;
  name: string;
  capacity: number;
  coordinatorFacultyId?: number | null;
  /** The batch's version as last fetched by the client -- rejected with a conflict if it no
   *  longer matches the current row (someone else changed it since). */
  version: number;
}

export interface BatchStudent {
  studentId: number;
  studentName: string;
  rollNumber: string;
}

export interface BatchLifecycleImpact {
  enrolledStudents: number;
  classScheduleCount: number;
  rotationAssignmentCount: number;
  escortAssignmentCount: number;
  sessionOccurrenceCount: number;
}

export function impactHasAny(impact: BatchLifecycleImpact): boolean {
  return impact.enrolledStudents > 0 || impact.classScheduleCount > 0
    || impact.rotationAssignmentCount > 0 || impact.escortAssignmentCount > 0
    || impact.sessionOccurrenceCount > 0;
}

export function describeImpact(impact: BatchLifecycleImpact): string {
  const parts: string[] = [];
  if (impact.enrolledStudents > 0) parts.push(`${impact.enrolledStudents} enrolled student(s)`);
  if (impact.classScheduleCount > 0) parts.push(`${impact.classScheduleCount} timetable slot(s)`);
  if (impact.rotationAssignmentCount > 0) parts.push(`${impact.rotationAssignmentCount} rotation assignment(s)`);
  if (impact.escortAssignmentCount > 0) parts.push(`${impact.escortAssignmentCount} escort assignment(s)`);
  if (impact.sessionOccurrenceCount > 0) parts.push(`${impact.sessionOccurrenceCount} scheduled session(s)`);
  return parts.join(', ');
}
