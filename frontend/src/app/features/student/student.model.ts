export interface Student {
  id: number;
  rollNumber: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone?: string;
  programId: number;
  programName: string;
  semester: number;
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
  fatherName?: string;
  motherName?: string;
  parentMobile?: string;
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
  semesterNumber: number;
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
  semesterNumber: number;
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
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  programId: number;
  semester: number;
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
  fatherName?: string;
  motherName?: string;
  parentMobile?: string;
  address?: {
    postalAddress?: string;
    street?: string;
    city?: string;
    district?: string;
    state?: string;
    pincode?: string;
  };
}
