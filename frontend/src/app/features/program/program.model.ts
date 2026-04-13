import { Department } from '../department/department.model';

export type DegreeType = 'BACHELOR' | 'MASTER' | 'DOCTORATE' | 'DIPLOMA' | 'CERTIFICATE';

export interface Program {
  id: number;
  name: string;
  code: string;
  degreeType: DegreeType;
  durationYears: number;
  department: Department;
  createdAt: string;
  updatedAt: string;
}

export interface ProgramRequest {
  name: string;
  code: string;
  degreeType: DegreeType;
  durationYears: number;
  departmentId: number;
}

export const DEGREE_TYPES: { value: DegreeType; label: string }[] = [
  { value: 'BACHELOR', label: 'Bachelor' },
  { value: 'MASTER', label: 'Master' },
  { value: 'DOCTORATE', label: 'Doctorate' },
  { value: 'DIPLOMA', label: 'Diploma' },
  { value: 'CERTIFICATE', label: 'Certificate' },
];
