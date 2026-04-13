export interface Examination {
  id: number;
  name: string;
  courseId: number;
  courseName: string;
  examType: string;
  date?: string;
  duration?: number;
  maxMarks?: number;
  semesterId?: number;
  semesterName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ExaminationRequest {
  name: string;
  courseId: number;
  examType: string;
  date?: string;
  duration?: number;
  maxMarks?: number;
  semesterId?: number;
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
