export interface AdmissionRequest {
  studentId: number;
  joiningAcademicYearId: number;
  applicationDate: string;
  status?: string;
  declarationPlace?: string;
  declarationDate?: string;
  parentConsentGiven?: boolean;
  applicantConsentGiven?: boolean;
}

export interface AdmissionResponse {
  id: number;
  studentId: number;
  studentName: string;
  admissionNumber: string | null;
  rollNumber: string | null;
  programName: string | null;
  courseName: string | null;
  yearOfStudy: number | null;
  studentStatus: string | null;
  joiningAcademicYearId: number;
  joiningAcademicYearName: string;
  expectedCompletionYear: number | null;
  applicationDate: string;
  declarationPlace: string | null;
  declarationDate: string | null;
  parentConsentGiven: boolean | null;
  applicantConsentGiven: boolean | null;
  createdAt: string;
  updatedAt: string;
}

export interface AcademicQualificationRequest {
  qualificationType: string;
  schoolName?: string;
  majorSubject?: string;
  totalMarks?: number;
  percentage?: number;
  monthAndYearOfPassing?: string;
  universityOrBoard?: string;
}

export interface AcademicQualificationResponse {
  id: number;
  admissionId: number;
  qualificationType: string;
  schoolName: string | null;
  majorSubject: string | null;
  totalMarks: number | null;
  percentage: number | null;
  monthAndYearOfPassing: string | null;
  universityOrBoard: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdmissionDocumentResponse {
  id: number;
  admissionId: number;
  documentType: string;
  fileName: string | null;
  storageKey: string | null;
  uploadedAt: string | null;
  originalSubmitted: boolean | null;
  verifiedBy: string | null;
  verifiedAt: string | null;
  verificationStatus: string;
  createdAt: string;
  updatedAt: string;
  hasFile: boolean;
  remarks: string | null;
  contentType: string | null;
  fileSize: number | null;
}


export const ADMISSION_STATUSES = [
  'SUBMITTED',
  'UNDER_REVIEW',
  'APPROVED',
  'REJECTED',
  'ENROLLED',
  'WITHDRAWN',
];

export const QUALIFICATION_TYPES = [
  'SSLC',
  'HSC',
  'DIPLOMA',
  'UG',
  'PG',
  'OTHER',
];
