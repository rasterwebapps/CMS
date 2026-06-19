import { Program } from '../program/program.model';

export interface Course {
  id: number;
  name: string;
  code: string;
  specialization: string | null;
  rollNumberCode: string | null;
  isActive: boolean;
  program: Program;
  createdAt: string;
  updatedAt: string;
}

export interface CourseRequest {
  name: string;
  code: string;
  specialization: string | null;
  rollNumberCode: string;
  programId: number;
  isActive?: boolean;
}

export interface CourseStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface CourseStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
