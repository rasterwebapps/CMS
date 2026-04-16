import { Program } from '../program/program.model';

export interface Course {
  id: number;
  name: string;
  code: string;
  specialization: string | null;
  program: Program;
  createdAt: string;
  updatedAt: string;
}

export interface CourseRequest {
  name: string;
  code: string;
  specialization: string | null;
  programId: number;
}
