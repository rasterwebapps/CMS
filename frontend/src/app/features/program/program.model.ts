import { Department } from '../department/department.model';

export interface Program {
  id: number;
  name: string;
  code: string;
  durationYears: number;
  departments: Department[];
  createdAt: string;
  updatedAt: string;
}

export interface ProgramRequest {
  name: string;
  code: string;
  durationYears: number;
  departmentIds?: number[];
}
