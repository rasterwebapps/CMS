export interface Batch {
  id: number;
  courseOfferingId: number;
  name: string;
  capacity: number;
  enrolledCount: number;
  termInstanceId: number;
  coordinatorFacultyId: number | null;
  coordinatorFacultyName: string | null;
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

export interface BatchAutoCreateRequest {
  courseOfferingId: number;
  count: number;
  capacity: number;
}

export interface BatchStudent {
  studentId: number;
  studentName: string;
  rollNumber: string;
}
