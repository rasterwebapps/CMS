import { Program } from '../program/program.model';

export interface Course {
  id: number;
  name: string;
  code: string;
  specialization: string | null;
  rollNumberCode: string | null;
  admissionNumberCode: string | null;
  program: Program;
  createdAt: string;
  updatedAt: string;
}

export interface CourseRequest {
  name: string;
  code: string;
  specialization: string | null;
  admissionNumberCode: string | null;
  programId: number;
}
