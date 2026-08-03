export interface SyllabusUnitPlan {
  unitId: number;
  unitNumber: number;
  title: string;
  plannedCompletionDate: string;
  plannedCumulativeHours: number;
  sequenceIndex: number;
}

export interface UnitVariance {
  unitId: number;
  unitNumber: number;
  title: string;
  plannedCompletionDate: string | null;
  projectedOrActualDate: string | null;
  completed: boolean;
  /** Positive = behind schedule, negative = ahead, null = not enough information yet. */
  varianceDays: number | null;
}

export interface SubjectShortfall {
  courseOfferingId: number;
  subjectName: string;
  subjectCode: string;
  remainingShortfallHours: number;
}

export interface PortionShortfall {
  termInstanceId: number;
  cohortId: number;
  bufferHours: number;
  totalShortfallHours: number;
  atRisk: boolean;
  subjects: SubjectShortfall[];
}
