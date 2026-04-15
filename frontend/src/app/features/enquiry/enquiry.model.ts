export interface Enquiry {
  id: number;
  name: string;
  email: string;
  phone: string;
  programId: number;
  programName: string;
  enquiryDate: string;
  source: string;
  status: string;
  agentId: number | null;
  agentName: string | null;
  referralTypeId: number | null;
  referralTypeName: string | null;
  referralGuidelineValue: number | null;
  assignedTo: string;
  remarks: string;
  feeDiscussedAmount: number | null;
  feeGuidelineTotal: number | null;
  referralAdditionalAmount: number | null;
  finalCalculatedFee: number | null;
  yearWiseFees: string | null;
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
  enquiryDate: string;
  source: string;
  status?: string;
  agentId?: number;
  referralTypeId?: number;
  assignedTo?: string;
  remarks?: string;
  feeDiscussedAmount?: number;
  feeGuidelineTotal?: number;
  referralAdditionalAmount?: number;
  finalCalculatedFee?: number;
  yearWiseFees?: string;
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
