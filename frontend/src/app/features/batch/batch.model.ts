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
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface BatchRequest {
  courseOfferingId: number;
  name: string;
  capacity: number;
  coordinatorFacultyId?: number | null;
}

export interface BatchStudent {
  studentId: number;
  studentName: string;
  rollNumber: string;
}
