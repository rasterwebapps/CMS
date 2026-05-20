export interface FeeStructureItemRequest {
  feeType: string;
  amount: number;
  description?: string;
  isMandatory?: boolean;
  isActive?: boolean;
  yearAmounts?: YearAmountRequest[];
}

export interface BulkFeeStructureRequest {
  programId: number;
  academicYearId: number;
  courseId?: number;
  items: FeeStructureItemRequest[];
}

export interface FeeStructure {
  id: number;
  programId: number;
  programName: string;
  courseId: number | null;
  courseName: string | null;
  academicYearId: number;
  academicYearName: string;
  feeType: string;
  amount: number;
  description: string;
  isMandatory: boolean;
  isActive: boolean;
  yearAmounts: YearAmountResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface GroupedFeeStructure {
  programId: number;
  programName: string;
  courseId: number | null;
  courseName: string | null;
  academicYearId: number;
  academicYearName: string;
  totalAmount: number;
  items: FeeStructure[];
}

export interface FeeStructureRequest {
  programId: number;
  academicYearId: number;
  feeType: string;
  amount: number;
  description?: string;
  isMandatory?: boolean;
  isActive?: boolean;
  courseId?: number;
  yearAmounts?: YearAmountRequest[];
}

export interface YearAmountResponse {
  id: number;
  yearNumber: number;
  yearLabel: string;
  amount: number;
}

export interface YearAmountRequest {
  yearNumber: number;
  yearLabel: string;
  amount: number;
}

export interface FeePayment {
  id: number;
  studentId: number;
  studentName: string;
  feeStructureId: number;
  feeStructureName: string;
  amountPaid: number;
  paymentDate: string;
  paymentMethod: string;
  transactionId?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface FeePaymentRequest {
  studentId: number;
  feeStructureId: number;
  amountPaid: number;
  paymentDate: string;
  paymentMethod: string;
  transactionId?: string;
  status?: string;
}

// Student Fee Allocation
export interface StudentFeeAllocation {
  id: number;
  studentId: number;
  studentName: string;
  rollNumber: string;
  programId: number;
  programName: string;
  totalFee: number;
  discountAmount: number;
  discountReason: string;
  agentCommission: number;
  netFee: number;
  status: string;
  finalizedAt: string;
  finalizedBy: string;
  installmentFees: InstallmentFeeDetail[];
  createdAt: string;
  updatedAt: string;
}

export interface InstallmentFeeDetail {
  id: number;
  yearNumber: number;
  sequence: number;
  installmentLabel: string;
  amount: number;
  dueDate: string;
  amountPaid: number;
  pendingAmount: number;
  penaltyAmount: number;
  paymentStatus: string;
}

export interface InstallmentPaymentDetail {
  installmentLabel: string;
  yearNumber: number;
  sequence: number;
  amountApplied: number;
}

export interface StudentFeeAllocationRequest {
  studentId: number;
  totalFee: number;
  discountAmount?: number;
  discountReason?: string;
  agentCommission?: number;
  yearFees: YearFee[];
}

export interface YearFee {
  yearNumber: number;
  amount: number;
  dueDate: string;
}

export interface CollectPaymentRequest {
  amount: number;
  paymentDate: string;
  paymentMode: string;
  transactionReference?: string;
  remarks?: string;
}

export interface CollectPaymentResponse {
  receiptNumber: string;
  studentId: number;
  studentName: string;
  rollNumber: string;
  amountPaid: number;
  paymentDate: string;
  paymentMode: string;
  transactionReference: string;
  remarks: string;
  allocationSummary: string;
  installmentBreakdown: InstallmentPaymentDetail[];
  feeCategory: 'TUITION_ONLY' | 'TUITION_AND_HOSTEL' | null;
  createdAt: string;
}

export interface PenaltyResponse {
  studentId: number;
  studentName: string;
  rollNumber: string;
  totalPenalty: number;
  penalties: PenaltyDetail[];
}

export interface PenaltyDetail {
  id: number;
  installmentFeeId: number;
  installmentLabel: string;
  yearNumber: number;
  dailyRate: number;
  penaltyStartDate: string;
  penaltyEndDate: string;
  overdueDays: number;
  totalPenalty: number;
  isPaid: boolean;
}

export interface FeeExplorerResult {
  students: StudentFeeSummary[];
}

export interface StudentFeeSummary {
  studentId: number;
  studentName: string;
  rollNumber: string;
  programName: string;
  durationYears: number;
  totalFee: number;
  totalPaid: number;
  totalPending: number;
  totalPenalty: number;
  allocationStatus: string;
}

export interface EnquiryYearFee {
  yearNumber: number;
  amount: number;
  suggestedDueDate: string;
}

export interface CreateAllocationYearFee {
  yearNumber: number;
  amount: number;
}

export interface CreateAllocationRequest {
  studentId: number;
  totalFee: number;
  discountAmount?: number;
  discountReason?: string;
  agentCommission?: number;
  yearFees: CreateAllocationYearFee[];
}

export interface Receipt {
  id: number;
  receiptNumber: string;
  studentId: number;
  studentName: string;
  rollNumber: string;
  installmentFeeId: number;
  installmentLabel: string;
  yearNumber: number;
  amountPaid: number;
  paymentDate: string;
  paymentMode: string;
  transactionReference: string;
  remarks: string;
  createdAt: string;
}

export interface ReceiptSummary {
  receiptNumber: string;
  studentId: number;
  studentName: string;
  rollNumber: string;
  totalAmountPaid: number;
  paymentDate: string;
  paymentMode: string;
  transactionReference: string | null;
  remarks: string | null;
  installmentsCovered: string;
  installmentBreakdown: InstallmentPaymentDetail[];
  createdAt: string;
}

/**
 * UI display model used by FeeReceiptDialogComponent.
 * Normalised from both enquiry payment and student payment API responses.
 */
export interface ReceiptDisplayData {
  receiptNumber: string;
  /** 'ENQUIRY' = pre-enrollment payment; 'STUDENT' = regular student installment */
  payerType: 'ENQUIRY' | 'STUDENT';
  payerName: string;
  /** Roll number for students; null for enquiry payments */
  payerIdentifier: string | null;
  programName: string | null;
  amountPaid: number;
  paymentDate: string;
  paymentMode: string;
  transactionReference?: string | null;
  remarks?: string | null;
  /** Human-readable installment(s) covered, e.g. "Year 1 – Semester 1" */
  installmentsCovered: string;
  installmentBreakdown: Array<{ label: string; amount: number }>;
  feeCategory?: 'TUITION_ONLY' | 'TUITION_AND_HOSTEL' | null;
}

/**
 * Unified receipt summary — combines student fee payments and enquiry
 * pre-enrollment payments into a single list row. Matches the backend
 * {@code UnifiedReceiptResponse} DTO returned by {@code GET /api/v1/receipts}.
 */
export interface UnifiedReceiptSummary {
  id: number;
  receiptNumber: string;
  payerType: 'STUDENT' | 'ENQUIRY';
  payerId: number;
  payerName: string;
  payerIdentifier: string | null;   // roll number for students, null for enquiries
  admissionNumber: string | null;
  programName: string | null;
  amountPaid: number;
  paymentDate: string;
  paymentMode: string;
  transactionReference: string | null;
  remarks: string | null;
  installmentsCovered: string | null;
  collectedBy: string | null;
  feeCategory: 'TUITION_ONLY' | 'TUITION_AND_HOSTEL' | null;
  createdAt: string;
}
