export interface Enquiry {
  id: number;
  name: string;
  email: string;
  phone: string;
  programId: number;
  programName: string;
  courseId: number | null;
  courseName: string | null;
  enquiryDate: string;
  status: string;
  agentId: number | null;
  agentName: string | null;
  referralTypeId: number | null;
  referralTypeName: string | null;
  referralCommissionAmount: number | null;
  referralHasCommission: boolean | null;
  remarks: string;
  feeDiscussedAmount: number | null;
  feeGuidelineTotal: number | null;
  referralAdditionalAmount: number | null;
  finalCalculatedFee: number | null;
  yearWiseFees: string | null;
  studentType: 'DAY_SCHOLAR' | 'HOSTELER' | null;
  finalizedTotalFee: number | null;
  finalizedDiscountAmount: number | null;
  finalizedDiscountReason: string | null;
  finalizedNetFee: number | null;
  finalizedBy: string | null;
  finalizedAt: string | null;
  convertedStudentId: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface EnquiryRequest {
  name: string;
  email?: string;
  phone?: string;
  programId?: number;
  courseId?: number;
  enquiryDate: string;
  referralTypeId: number;
  status?: string;
  agentId?: number;
  remarks?: string;
  feeDiscussedAmount?: number;
  feeGuidelineTotal?: number;
  referralAdditionalAmount?: number;
  finalCalculatedFee?: number;
  yearWiseFees?: string;
  studentType?: 'DAY_SCHOLAR' | 'HOSTELER';
}

export interface FeeFinalizationRequest {
  totalFee: number;
  discountAmount?: number;
  discountReason?: string;
  yearWiseFees?: string;
}

export interface FeeFinalizationResponse {
  enquiryId: number;
  finalizedTotalFee: number;
  finalizedDiscountAmount: number;
  finalizedDiscountReason: string;
  finalizedNetFee: number;
  finalizedBy: string;
  finalizedAt: string;
  status: string;
}

export interface EnquiryDocument {
  id: number;
  enquiryId: number;
  documentType: string;
  status: string;
  remarks: string;
  verifiedBy: string;
  verifiedAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface EnquiryDocumentRequest {
  documentType: string;
  status?: string;
  remarks?: string;
}

export interface EnquiryPaymentRequest {
  amountPaid: number;
  paymentDate: string;
  paymentMode: string;
  transactionReference?: string;
  remarks?: string;
}

export interface EnquiryPaymentResponse {
  id: number;
  enquiryId: number;
  enquiryName: string;
  amountPaid: number;
  paymentDate: string;
  paymentMode: string;
  transactionReference: string | null;
  remarks: string | null;
  receiptNumber: string;
  collectedBy: string | null;
  newStatus: string;
  createdAt: string;
}

export interface EnquiryStatusHistoryResponse {
  id: number;
  enquiryId: number;
  fromStatus: string | null;
  toStatus: string;
  changedBy: string;
  changedAt: string;
  remarks: string | null;
}

export interface EnquirySummaryResponse {
  enquiry: Enquiry;
  totalAmountPaid: number;
  outstandingAmount: number;
  documentCount: number;
  documentTypes: string[];
}

export interface MissingDocumentsResponse {
  allSubmitted: boolean;
  missingDocumentTypes: string[];
}

export interface EnquiryConversionRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  semester: number;
  admissionDate: string;
  academicYearFrom: number;
  academicYearTo: number;
  applicationDate: string;
  parentConsentGiven?: boolean;
  applicantConsentGiven?: boolean;

  // Student personal information
  dateOfBirth?: string | null;
  gender?: 'MALE' | 'FEMALE' | 'OTHER' | null;
  aadharNumber?: string | null;

  // Student demographics
  nationality?: string | null;
  religion?: string | null;
  communityCategory?: 'SC' | 'ST' | 'BC' | 'MBC' | 'DNC' | 'OC' | 'OTHERS' | null;
  caste?: string | null;
  bloodGroup?:
    | 'A_POSITIVE' | 'A_NEGATIVE'
    | 'B_POSITIVE' | 'B_NEGATIVE'
    | 'O_POSITIVE' | 'O_NEGATIVE'
    | 'AB_POSITIVE' | 'AB_NEGATIVE'
    | null;

  // Student family information
  fatherName?: string | null;
  motherName?: string | null;
  parentMobile?: string | null;

  // Student address
  address?: {
    postalAddress?: string | null;
    street?: string | null;
    city?: string | null;
    district?: string | null;
    state?: string | null;
    pincode?: string | null;
  } | null;

  // Admission declaration
  declarationPlace?: string | null;
  declarationDate?: string | null;
}

export interface EnquiryConversionPrefillResponse {
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
  programId: number | null;
  programName: string | null;
  courseId: number | null;
  courseName: string | null;
  suggestedSemester: number;
  suggestedAcademicYearFrom: number;
  suggestedAcademicYearTo: number;
  suggestedApplicationDate: string;
}

export interface YearFeeStatus {
  yearNumber: number;
  allocatedFee: number;
  paidAmount: number;
  outstanding: number;
}

export interface EnquiryYearWiseFeeStatusResponse {
  enquiryId: number;
  totalFee: number;
  totalPaid: number;
  totalOutstanding: number;
  yearBreakdown: YearFeeStatus[];
}
