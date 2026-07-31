import { AttendanceComponentType } from '../curriculum/curriculum-version.model';

/** One selectable unit in the Log Progress dialog, with its aggregate state across every session
 *  logged so far (not just the date being edited) so the picker can default to the actual
 *  current unit instead of a flat list with no sense of where things stand. */
export interface UnitPickerOption {
  id: number;
  unitNumber: number;
  title: string;
  componentType: AttendanceComponentType;
  plannedHours: number | null;
  hoursLoggedSoFar: number;
  markedComplete: boolean;
}

export interface UnitCoverage {
  unitId: number;
  unitNumber: number;
  title: string;
  hoursCovered: number | null;
  markedComplete: boolean;
}

export interface SessionOccurrence {
  id: number;
  classScheduleId: number;
  occurrenceDate: string;
  unitCoverages: UnitCoverage[];
  remarks: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UnitCoverageEntry {
  unitId: number;
  hoursCovered: number | null;
  markedComplete: boolean;
}

export interface LogProgressRequest {
  classScheduleId: number;
  occurrenceDate: string;
  units: UnitCoverageEntry[];
  remarks?: string | null;
}

export interface UnitProgress {
  unitId: number;
  unitNumber: number;
  title: string;
  componentType: AttendanceComponentType;
  plannedHours: number | null;
  hoursLogged: number;
  completed: boolean;
  coveredDates: string[];
}

export interface OfferingProgress {
  courseOfferingId: number;
  subjectName: string;
  subjectCode: string;
  totalUnits: number;
  coveredUnitCount: number;
  percentComplete: number;
  units: UnitProgress[];
}

export interface SubjectProgressSummary {
  courseOfferingId: number;
  subjectName: string;
  subjectCode: string;
  totalUnits: number;
  coveredUnitCount: number;
  percentComplete: number;
}

export interface TermProgressSummary {
  termInstanceId: number;
  subjects: SubjectProgressSummary[];
  overallPercentComplete: number;
}
