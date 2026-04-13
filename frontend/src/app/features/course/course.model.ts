import { Program } from '../program/program.model';

export interface Course {
  id: number;
  name: string;
  code: string;
  credits: number;
  theoryCredits: number;
  labCredits: number;
  semester: number;
  program: Program;
  createdAt: string;
  updatedAt: string;
}

export interface CourseRequest {
  name: string;
  code: string;
  credits: number;
  theoryCredits: number;
  labCredits: number;
  semester: number;
  programId: number;
}
