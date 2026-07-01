export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type DiscountType = 'PERCENTAGE' | 'FIXED_AMOUNT' | 'FULL_WAIVER';
export type ScholarshipStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'ON_HOLD' | 'CANCELLED';
export type DisbursementFrequency = 'ANNUAL' | 'TERM_BASED' | 'ONE_TIME';
export type DisbursementMode = 'DIRECT_CREDIT' | 'FEE_WAIVER' | 'CHEQUE';

export interface ScholarshipType {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  govtScheme: boolean;
  schemeCode?: string | null;
  discountType: DiscountType;
  discountValue?: number | null;
  maxAmountPerYear?: number | null;
  renewalRequired: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ScholarshipTypeRequest {
  code: string;
  name: string;
  description?: string | null;
  govtScheme?: boolean;
  schemeCode?: string | null;
  discountType: DiscountType;
  discountValue?: number | null;
  maxAmountPerYear?: number | null;
  renewalRequired?: boolean;
  active?: boolean;
}

export interface ScholarshipTypeStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface ScholarshipTypeStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}

export interface ScholarshipEligibility {
  id: number;
  studentId: number;
  studentName: string;
  communityCategory?: string | null;
  caste?: string | null;
  isFirstGraduate: boolean;
  isMeritBased: boolean;
  isSportsQuota: boolean;
  isEconomicallyWeaker: boolean;
  annualFamilyIncome?: number | null;
  incomeCertificateNumber?: string | null;
  incomeCertIssuingAuthority?: string | null;
  incomeCertIssueDate?: string | null;
  communityCertificateNumber?: string | null;
  commCertIssuingAuthority?: string | null;
  commCertIssueDate?: string | null;
  firstGraduateCertificateNumber?: string | null;
  firstGradCertIssuingAuthority?: string | null;
  firstGradCertIssueDate?: string | null;
  fatherEducation?: string | null;
  motherEducation?: string | null;
  verifiedBy?: string | null;
  verifiedAt?: string | null;
  verificationRemarks?: string | null;
  eligibleScholarships: ScholarshipType[];
  createdAt: string;
  updatedAt: string;
}

export interface ScholarshipEligibilityRequest {
  isFirstGraduate?: boolean;
  isMeritBased?: boolean;
  isSportsQuota?: boolean;
  isEconomicallyWeaker?: boolean;
  annualFamilyIncome?: number | null;
  incomeCertificateNumber?: string | null;
  incomeCertIssuingAuthority?: string | null;
  incomeCertIssueDate?: string | null;
  communityCertificateNumber?: string | null;
  commCertIssuingAuthority?: string | null;
  commCertIssueDate?: string | null;
  firstGraduateCertificateNumber?: string | null;
  firstGradCertIssuingAuthority?: string | null;
  firstGradCertIssueDate?: string | null;
  fatherEducation?: string | null;
  motherEducation?: string | null;
}

export interface ScholarshipApplicationRequest {
  scholarshipTypeId: number;
  academicYearId?: number | null;
  applicationRemarks?: string | null;
}

export interface ScholarshipApplication {
  id: number;
  studentId: number;
  studentName: string;
  rollNumber?: string | null;
  scholarshipTypeId: number;
  scholarshipCode: string;
  scholarshipName: string;
  academicYearId: number;
  academicYearName: string;
  applicationDate: string;
  applicationRemarks?: string | null;
  status: ScholarshipStatus;
  approvedBy?: string | null;
  approvedAt?: string | null;
  rejectionReason?: string | null;
  approvedAmount?: number | null;
  disbursementFrequency?: DisbursementFrequency | null;
  validFrom?: string | null;
  validTill?: string | null;
  renewedFromId?: number | null;
  renewalRequired: boolean;
  createdBy?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ScholarshipApprovalRequest {
  approvedAmount: number;
  disbursementFrequency?: DisbursementFrequency | null;
  validFrom?: string | null;
  validTill?: string | null;
  remarks?: string | null;
}

export interface ScholarshipRejectionRequest {
  reason: string;
}

export interface DisbursementRequest {
  academicYearId?: number | null;
  termNumber?: number | null;
  amount: number;
  disbursementDate: string;
  disbursementMode: DisbursementMode;
  transactionReference?: string | null;
  chequeNumber?: string | null;
  bankName?: string | null;
  remarks?: string | null;
}

export interface ScholarshipDisbursement {
  id: number;
  studentScholarshipId: number;
  studentId: number;
  studentName: string;
  academicYearId?: number | null;
  academicYearName?: string | null;
  termNumber?: number | null;
  amount: number;
  disbursementDate: string;
  disbursementMode: DisbursementMode;
  transactionReference?: string | null;
  chequeNumber?: string | null;
  bankName?: string | null;
  remarks?: string | null;
  disbursedBy?: string | null;
  createdAt: string;
}

