import { Department } from '../department/department.model';

export type ProgramLevel = 'UNDERGRADUATE' | 'POSTGRADUATE' | 'DIPLOMA' | 'CERTIFICATE' | 'DOCTORAL';

export interface Program {
  id: number;
  name: string;
  code: string;
  programLevel: ProgramLevel;
  durationYears: number;
  departments: Department[];
  createdAt: string;
  updatedAt: string;
}

export interface ProgramRequest {
  name: string;
  code: string;
  programLevel: ProgramLevel;
  durationYears: number;
  departmentIds: number[];
}

export const PROGRAM_LEVELS: { value: ProgramLevel; label: string }[] = [
  { value: 'UNDERGRADUATE', label: 'Undergraduate' },
  { value: 'POSTGRADUATE', label: 'Postgraduate' },
  { value: 'DIPLOMA', label: 'Diploma' },
  { value: 'CERTIFICATE', label: 'Certificate' },
  { value: 'DOCTORAL', label: 'Doctoral' },
];
