export interface ImportDefaults {
  defaultJoiningAcademicYearId: number | null;
  defaultStudentType: string;
  defaultNationality: string;
  defaultState: string;
  defaultSemester: number;
  skipErroredRows: boolean;
}

export interface ImportRowError {
  sheet: string;
  rowNumber: number;
  column: string;
  message: string;
  severity: 'ERROR' | 'WARNING';
}

export interface ImportValidationResult {
  studentsTotal: number;
  studentsValid: number;
  qualificationsTotal: number;
  qualificationsValid: number;
  feeHistoryTotal: number;
  feeHistoryValid: number;
  errors: ImportRowError[];
  warnings: ImportRowError[];
}

export interface ImportExecuteResult {
  studentsImported: number;
  studentsSkipped: number;
  admissionsCreated: number;
  qualificationsImported: number;
  feeAllocationsCreated: number;
  paymentsImported: number;
  errors: ImportRowError[];
}
