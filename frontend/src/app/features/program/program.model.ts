export interface Program {
  id: number;
  name: string;
  code: string;
  durationYears: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProgramRequest {
  name: string;
  code: string;
  durationYears: number;
}
