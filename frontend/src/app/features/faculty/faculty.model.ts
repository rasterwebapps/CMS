
export type FacultyStatus =
  | 'ACTIVE'
  | 'ON_LEAVE'
  | 'SABBATICAL'
  | 'RESIGNED'
  | 'RETIRED'
  | 'TERMINATED';

export type FacultyType = 'TEACHING' | 'NON_TEACHING';

export type Gender = 'MALE' | 'FEMALE' | 'OTHER';

export type MaritalStatus = 'SINGLE' | 'MARRIED' | 'DIVORCED' | 'WIDOWED' | 'OTHER';

export type BankAccountType = 'SAVINGS' | 'CURRENT' | 'SALARY';

export type FacultyQualification = 'UG' | 'PG' | 'MPHIL' | 'PHD' | 'OTHER';

export type DocumentVerificationStatus =
  | 'NOT_UPLOADED'
  | 'UPLOADED'
  | 'VERIFIED'
  | 'REJECTED';

export type FacultyDocumentReviewFilter =
  | 'ALL'
  | 'NEEDS_VERIFICATION'
  | 'REJECTED'
  | 'MISSING_REQUIRED'
  | 'FULLY_VERIFIED'
  | 'NO_DOCUMENTS'
  | 'HAS_ANY_DOCUMENTS';

export interface FacultyDocumentReviewSummary {
  totalDocumentCount: number;
  requiredDocumentCount: number;
  pendingVerificationCount: number;
  rejectedCount: number;
  missingRequiredCount: number;
  verifiedRequiredCount: number;
  hasAnyDocuments: boolean;
  hasPendingVerification: boolean;
  hasRejectedDocuments: boolean;
  allRequiredDocumentsVerified: boolean;
}

export interface AddressDto {
  countryId?: number | null;
  postalAddress?: string;
  street?: string;
  city?: string;
  district?: string;
  state?: string;
  pincode?: string;
}

export interface Faculty {
  id: number;
  employeeCode: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone?: string;
  bio?: string;
  emergencyContactName?: string;
  emergencyContactRelationship?: string;
  emergencyContactPhone?: string;
  specialityId: number;
  specialityName: string;
  designationId: number;
  designationName: string;
  specialization?: string;
  labExpertise?: string;
  joiningDate: string;
  status: FacultyStatus;
  facultyType?: FacultyType;
  highestQualification?: FacultyQualification;
  nrtsNumber?: string;
  panNumber?: string;
  aadhaarNumber?: string;
  dateOfBirth?: string;
  gender?: Gender;
  maritalStatus?: MaritalStatus;
  nationality?: string;
  religion?: string;
  bloodGroup?: string;
  bankAccountNumber?: string;
  bankIfscCode?: string;
  bankBranch?: string;
  bankName?: string;
  bankAccountHolder?: string;
  bankAccountType?: BankAccountType;
  address?: AddressDto;
  teachingExperienceUgYears?: number;
  teachingExperiencePgYears?: number;
  teachingExperiencePhdYears?: number;
  clinicalExperienceUgYears?: number;
  clinicalExperiencePgYears?: number;
  clinicalExperiencePhdYears?: number;
  createdAt: string;
  updatedAt: string;
  documentReview?: FacultyDocumentReviewSummary;
  commissionAmount?: number | null;
  /** Advisory-only, for the faculty capacity-planning report. Wins over the designation's default
   *  when set; undefined means "use the designation default", not zero. */
  plannedWeeklyHoursOverride?: number | null;
  /** Same override precedence as {@link plannedWeeklyHoursOverride}, feeds the daily hard cap. */
  plannedDailyHoursOverride?: number | null;
  /** Same override precedence as {@link plannedWeeklyHoursOverride}, feeds the continuous hard cap. */
  plannedContinuousHoursOverride?: number | null;
}

export interface FacultyRequest {
  employeeCode: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  specialityId: number;
  designationId: number;
  specialization?: string;
  labExpertise?: string;
  joiningDate: string;
  status?: FacultyStatus;
  facultyType?: FacultyType;
  highestQualification?: FacultyQualification;
  nrtsNumber?: string;
  panNumber?: string;
  aadhaarNumber?: string;
  dateOfBirth?: string;
  gender?: Gender;
  maritalStatus?: MaritalStatus;
  nationality?: string;
  religion?: string;
  bloodGroup?: string;
  bankAccountNumber?: string;
  bankIfscCode?: string;
  bankBranch?: string;
  bankName?: string;
  bankAccountHolder?: string;
  bankAccountType?: BankAccountType;
  address?: AddressDto;
  teachingExperienceUgYears?: number;
  teachingExperiencePgYears?: number;
  teachingExperiencePhdYears?: number;
  clinicalExperienceUgYears?: number;
  clinicalExperiencePgYears?: number;
  clinicalExperiencePhdYears?: number;
  commissionAmount?: number;
  plannedWeeklyHoursOverride?: number;
  plannedDailyHoursOverride?: number;
  plannedContinuousHoursOverride?: number;
}

export interface FacultyDocument {
  id: number;
  facultyId: number;
  documentType: string;
  status: DocumentVerificationStatus;
  remarks?: string;
  verifiedBy?: string;
  verifiedAt?: string;
  createdAt: string;
  updatedAt: string;
  fileName?: string;
  contentType?: string;
  fileSize?: number;
  uploadedAt?: string;
  hasFile: boolean;
}


export const FACULTY_STATUS_OPTIONS: { value: FacultyStatus; label: string }[] = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'ON_LEAVE', label: 'On Leave' },
  { value: 'SABBATICAL', label: 'Sabbatical' },
  { value: 'RESIGNED', label: 'Resigned' },
  { value: 'RETIRED', label: 'Retired' },
  { value: 'TERMINATED', label: 'Terminated' },
];

export const FACULTY_DOCUMENT_REVIEW_FILTER_OPTIONS: {
  value: FacultyDocumentReviewFilter;
  label: string;
}[] = [
  { value: 'ALL', label: 'All Document States' },
  { value: 'NEEDS_VERIFICATION', label: 'Needs Verification' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'MISSING_REQUIRED', label: 'Missing Required' },
  { value: 'FULLY_VERIFIED', label: 'Fully Verified' },
  { value: 'NO_DOCUMENTS', label: 'No Documents' },
  { value: 'HAS_ANY_DOCUMENTS', label: 'Has Any Documents' },
];

export const FACULTY_TYPE_OPTIONS: { value: FacultyType; label: string }[] = [
  { value: 'TEACHING', label: 'Teaching' },
  { value: 'NON_TEACHING', label: 'Non-Teaching' },
];

export const GENDER_OPTIONS: { value: Gender; label: string }[] = [
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' },
];

export const MARITAL_STATUS_OPTIONS: { value: MaritalStatus; label: string }[] = [
  { value: 'SINGLE', label: 'Single' },
  { value: 'MARRIED', label: 'Married' },
  { value: 'DIVORCED', label: 'Divorced' },
  { value: 'WIDOWED', label: 'Widowed' },
  { value: 'OTHER', label: 'Other' },
];

export const BANK_ACCOUNT_TYPE_OPTIONS: { value: BankAccountType; label: string }[] = [
  { value: 'SAVINGS', label: 'Savings' },
  { value: 'CURRENT', label: 'Current' },
  { value: 'SALARY', label: 'Salary' },
];

export interface FacultyDocumentTypeRequirement {
  id: number;
  documentType: string;
  documentTypeLabel: string;
  designationId?: number;
  designationName?: string;
  specialityId?: number;
  specialityName?: string;
  qualification?: FacultyQualification;
  qualificationLabel?: string;
  createdAt: string;
}

export interface FacultyDocumentTypeRequirementRequest {
  documentType: string;
  designationId?: number;
  specialityId?: number;
  qualification?: FacultyQualification;
}

export const FACULTY_QUALIFICATION_OPTIONS: { value: FacultyQualification; label: string }[] = [
  { value: 'UG', label: 'Under Graduate' },
  { value: 'PG', label: 'Post Graduate' },
  { value: 'MPHIL', label: 'M.Phil' },
  { value: 'PHD', label: 'Ph.D' },
  { value: 'OTHER', label: 'Other' },
];

/**
 * Faculty document checklist — collected during onboarding.
 * Mirrors the DocumentType enum values used by the backend.
 */
export interface FacultyDocumentSlot {
  type: string;
  label: string;
  group: 'Service' | 'Qualification' | 'Identity' | 'Experience';
}

export const FACULTY_DOCUMENT_SLOTS: FacultyDocumentSlot[] = [
  { type: 'APPOINTMENT_LETTER',              label: 'Appointment Letter',          group: 'Service' },
  { type: 'JOINING_REPORT',                  label: 'Joining Report',              group: 'Service' },
  { type: 'PROMOTION_LETTER',                label: 'Promotion Letter',            group: 'Service' },
  { type: 'RENEWAL_CERTIFICATE',             label: 'Renewal Certificate',         group: 'Service' },
  { type: 'UG_DEGREE',                       label: 'UG Degree',                   group: 'Qualification' },
  { type: 'PG_DEGREE',                       label: 'PG Degree',                   group: 'Qualification' },
  { type: 'UG_RNRM',                         label: 'UG RN/RM Registration',       group: 'Qualification' },
  { type: 'PG_RNRM',                         label: 'PG RN/RM Registration',       group: 'Qualification' },
  { type: 'TEACHING_EXPERIENCE_CERTIFICATE', label: 'Teaching Experience',         group: 'Experience' },
  { type: 'CLINICAL_EXPERIENCE_CERTIFICATE', label: 'Clinical Experience',         group: 'Experience' },
  { type: 'AADHAR_CARD',                     label: 'Aadhaar Card',                group: 'Identity' },
  { type: 'PAN_CARD',                        label: 'PAN Card',                    group: 'Identity' },
  { type: 'FACULTY_PHOTO',                   label: 'Faculty Photo',               group: 'Identity' },
  { type: 'SCANNED_SIGNATURE',               label: 'Scanned Signature',           group: 'Identity' },
  { type: 'E_SIGNATURE',                     label: 'e-Signature',                 group: 'Identity' },
];

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
