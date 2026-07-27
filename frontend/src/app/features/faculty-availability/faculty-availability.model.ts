export interface FacultyAvailabilityBlock {
  id: number;
  facultyId: number;
  facultyName: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  reason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface FacultyAvailabilityRequest {
  facultyId: number;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  reason?: string | null;
}
