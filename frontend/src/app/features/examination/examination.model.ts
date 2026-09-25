export interface Examination {
  id: number;
  name: string;
  subjectId: number;
  subjectName: string;
  examType: string;
  date?: string;
  duration?: number;
  maxMarks?: number;
  createdAt: string;
  updatedAt: string;
}

export interface ExaminationRequest {
  name: string;
  subjectId: number;
  examType: string;
  date?: string;
  duration?: number;
  maxMarks?: number;
}

export interface ExamResult {
  id: number;
  examinationId: number;
  examinationName: string;
  studentId: number;
  studentName: string;
  studentRollNumber: string;
  marksObtained?: number;
  grade?: string;
  status: string;
  outcome?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ExamResultRequest {
  examinationId: number;
  studentId: number;
  marksObtained?: number;
  grade?: string;
  status?: string;
}
