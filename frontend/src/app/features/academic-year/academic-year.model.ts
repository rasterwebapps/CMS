export interface AcademicYear {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  isCurrent: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AcademicYearRequest {
  name: string;
  startDate: string;
  endDate: string;
  isCurrent: boolean;
}

export type SemesterStatus = 'UPCOMING' | 'ONGOING' | 'COMPLETED';

export interface Semester {
  id: number;
  name: string;
  semesterNumber: number;
  startDate: string;
  endDate: string;
  status: SemesterStatus;
  academicYear: AcademicYear;
  createdAt: string;
  updatedAt: string;
}

export interface SemesterRequest {
  name: string;
  semesterNumber: number;
  startDate: string;
  endDate: string;
  academicYearId: number;
}

export type CalendarEventType = 'HOLIDAY' | 'EXAM' | 'CULTURAL' | 'SPORTS' | 'WORKSHOP' | 'OTHER';

export interface CalendarEvent {
  id: number;
  title: string;
  description?: string;
  startDate: string;
  endDate: string;
  eventType: CalendarEventType;
  academicYear: AcademicYear;
  semester?: Semester;
  createdAt: string;
  updatedAt: string;
}

export interface CalendarEventRequest {
  title: string;
  description?: string;
  startDate: string;
  endDate: string;
  eventType: CalendarEventType;
  academicYearId: number;
  semesterId?: number;
}

export type TermType = 'ODD' | 'EVEN';
export type TermInstanceStatus = 'PLANNED' | 'OPEN' | 'LOCKED';
export type LateFeeType = 'FLAT' | 'PER_DAY';

export interface TermInstance {
  id: number;
  academicYearId: number;
  academicYearName: string;
  termType: TermType;
  startDate: string;
  endDate: string;
  status: TermInstanceStatus;
  createdAt: string;
  updatedAt: string;
}

export interface TermInstanceUpdateRequest {
  startDate?: string | null;
  endDate?: string | null;
  status?: TermInstanceStatus | null;
}

export interface TermBillingSchedule {
  id: number;
  academicYearId: number;
  academicYearName: string;
  termType: TermType;
  dueDate: string;
  lateFeeType: LateFeeType;
  lateFeeAmount: number;
  graceDays: number;
  createdAt: string;
  updatedAt: string;
}

export interface TermBillingScheduleRequest {
  academicYearId: number;
  termType: TermType;
  dueDate: string;
  lateFeeType: LateFeeType;
  lateFeeAmount: number;
  graceDays: number;
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

export interface GenerateEnrollmentsResponse {
  enrollmentsCreated: number;
}

export type RegistrationStatus = 'REGISTERED' | 'DROPPED' | 'COMPLETED';

export interface CourseOffering {
  id: number;
  termInstanceId: number;
  termInstanceLabel: string;
  curriculumVersionId: number;
  curriculumVersionName: string;
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  semesterNumber: number;
  facultyId: number | null;
  sectionLabel: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CourseOfferingUpdateRequest {
  facultyId?: number | null;
  sectionLabel?: string | null;
}

export interface GenerateCourseOfferingsResponse {
  offeringsCreated: number;
}

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

export interface GenerateCourseRegistrationsResponse {
  registrationsCreated: number;
}

export type DemandStatus = 'UNPAID' | 'PARTIAL' | 'PAID' | 'WAIVED';

export interface FeeDemand {
  id: number;
  enrollmentId: number;
  studentId: number;
  studentName: string;
  cohortCode: string;
  termInstanceId: number;
  termInstanceLabel: string;
  academicYearId: number;
  academicYearName: string;
  totalAmount: number;
  dueDate: string;
  paidAmount: number;
  outstandingAmount: number;
  status: DemandStatus;
}

export interface GenerateDemandsResponse {
  demandsCreated: number;
}

export type PaymentMode =
  | 'CASH'
  | 'CARD'
  | 'UPI'
  | 'NET_BANKING'
  | 'BANK_TRANSFER'
  | 'CHEQUE'
  | 'DEMAND_DRAFT'
  | 'SCHOLARSHIP';

export interface TermFeePaymentRequest {
  feeDemandId: number;
  paymentDate: string;
  amountPaid: number;
  paymentMode: PaymentMode;
  remarks?: string;
}

export interface TermFeePayment {
  id: number;
  feeDemandId: number;
  studentName: string;
  paymentDate: string;
  amountPaid: number;
  lateFeeApplied: number;
  totalCollected: number;
  paymentMode: PaymentMode;
  receiptNumber: string;
  remarks?: string;
  demandStatus: DemandStatus;
  createdAt: string;
  updatedAt: string;
}

