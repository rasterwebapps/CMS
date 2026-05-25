export interface ProgramTransferDocumentInfo {
  documentId: number | null;
  documentType: string;
  documentTypeLabel: string;
  status: string;
}

export interface ProgramTransferAnalysis {
  studentId: number;
  studentName: string;
  oldProgramId: number;
  oldProgramName: string;
  newProgramId: number;
  newProgramName: string;
  retainedDocuments: ProgramTransferDocumentInfo[];
  irrelevantDocuments: ProgramTransferDocumentInfo[];
  missingDocuments: ProgramTransferDocumentInfo[];
}

export interface ProgramTransferRequest {
  newProgramId: number;
  documentIdsToReturn: number[];
  consentConfirmed: boolean;
  notes?: string;
}

export interface ProgramTransferRecord {
  id: number;
  studentId: number;
  studentName: string;
  oldProgramId: number;
  oldProgramName: string;
  newProgramId: number;
  newProgramName: string;
  transferredAt: string;
  transferredBy: string | null;
  consentConfirmed: boolean;
  notes: string | null;
}

export interface Student {
  id: number;
  rollNumber: string;
  admissionNumber?: string;
  universityRegistrationNumber?: string;
  umisNumber?: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone?: string;
  bio?: string;
  emergencyContactName?: string;
  emergencyContactRelationship?: string;
  emergencyContactPhone?: string;
  programId: number;
  programName: string;
  yearOfStudy: number;
  admissionDate: string;
  labBatch?: string;
  status: string;
  dateOfBirth?: string;
  gender?: string;
  nationality?: string;
  religion?: string;
  communityCategory?: string;
  caste?: string;
  bloodGroup?: string;
  physicalDisability?: boolean;
  fatherName?: string;
  fatherPhone?: string;
  fatherEmail?: string;
  motherName?: string;
  motherPhone?: string;
  motherEmail?: string;
  parentMobile?: string;
  isFirstGraduate?: boolean;
  fatherEducation?: string;
  motherEducation?: string;
  countryId?: number | null;
  postalAddress?: string;
  street?: string;
  city?: string;
  district?: string;
  state?: string;
  pincode?: string;
  createdAt: string;
  updatedAt: string;
}

export type EnrollmentStatus = 'ENROLLED' | 'COMPLETED' | 'DROPPED';

export interface StudentTermEnrollment {
  id: number;
  studentId: number;
  studentName: string;
  cohortId: number;
  cohortCode: string;
  termInstanceId: number;
  termInstanceLabel: string;
  termNumber: number;
  yearOfStudy: number;
  status: EnrollmentStatus;
}

export type RegistrationStatus = 'REGISTERED' | 'DROPPED' | 'COMPLETED';

export interface CourseRegistration {
  id: number;
  enrollmentId: number;
  studentId: number;
  studentName: string;
  cohortCode: string;
  courseOfferingId: number;
  subjectName: string;
  subjectCode: string;
  termNumber: number;
  status: RegistrationStatus;
  createdAt: string;
  updatedAt: string;
}

export type DemandStatus = 'UNPAID' | 'PARTIAL' | 'PAID' | 'WAIVED';

export interface TermFeePaymentSummary {
  id: number;
  paymentDate: string;
  amountPaid: number;
  lateFeeApplied: number;
  totalCollected: number;
  paymentMode: string;
  receiptNumber: string;
  remarks?: string;
}

export interface StudentLedgerEntry {
  demandId: number;
  termLabel: string;
  totalAmount: number;
  paidAmount: number;
  outstandingAmount: number;
  dueDate: string;
  status: DemandStatus;
  payments: TermFeePaymentSummary[];
}

export interface StudentFeeLedger {
  studentId: number;
  studentName: string;
  entries: StudentLedgerEntry[];
}

export interface StudentRequest {
  rollNumber: string;
  universityRegistrationNumber?: string;
  umisNumber?: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  programId: number;
  yearOfStudy: number;
  admissionDate: string;
  labBatch?: string;
  status?: string;
  dateOfBirth?: string;
  gender?: string;
  nationality?: string;
  religion?: string;
  communityCategory?: string;
  caste?: string;
  bloodGroup?: string;
  physicalDisability?: boolean;
  fatherName?: string;
  fatherPhone?: string;
  fatherEmail?: string;
  motherName?: string;
  motherPhone?: string;
  motherEmail?: string;
  parentMobile?: string;
  isFirstGraduate?: boolean;
  fatherEducation?: string;
  motherEducation?: string;
  address?: {
    countryId?: number | null;
    postalAddress?: string;
    street?: string;
    city?: string;
    district?: string;
    state?: string;
    pincode?: string;
  };
}
