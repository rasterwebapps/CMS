export interface FacultyAvailabilityBlock {
  id: number;
  facultyId: number;
  facultyName: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  reason: string | null;
  /** Both null means the block recurs indefinitely (no end date). Both set means it only applies
   *  weekly within that date range. */
  startDate: string | null;
  endDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface FacultyAvailabilityRequest {
  facultyId: number;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  /** Required — backend rejects a blank reason. */
  reason: string;
  /** Pass both together to scope the block to a set of weeks, or neither for indefinite. */
  startDate?: string | null;
  endDate?: string | null;
}
