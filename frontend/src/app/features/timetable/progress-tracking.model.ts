import { AttendanceComponentType } from '../curriculum/curriculum-version.model';

export interface SyllabusUnitOption {
  id: number;
  unitNumber: number;
  title: string;
  componentType: AttendanceComponentType;
  plannedHours: number | null;
}

export interface CoveredUnit {
  id: number;
  unitNumber: number;
  title: string;
}

export interface SessionOccurrence {
  id: number;
  classScheduleId: number;
  occurrenceDate: string;
  coveredUnits: CoveredUnit[];
  remarks: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface LogProgressRequest {
  classScheduleId: number;
  occurrenceDate: string;
  unitIds: number[];
  remarks?: string | null;
}

export interface UnitProgress {
  unitId: number;
  unitNumber: number;
  title: string;
  componentType: AttendanceComponentType;
  plannedHours: number | null;
  covered: boolean;
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
