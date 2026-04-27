export type ProgramStatus = 'ACTIVE' | 'INACTIVE';

export interface Program {
  id: number;
  name: string;
  code: string;
  durationYears: number;
  totalSemesters: number;
  status: ProgramStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ProgramRequest {
  name: string;
  code: string;
  durationYears: number;
  status?: ProgramStatus;
}
