export interface FeeState {
  id: number;
  name: string;
  code: string;
  isDefault: boolean;
  isFallback: boolean;
  sortOrder: number;
}

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
  quota: 'MANAGEMENT' | 'COUNSELLING';
  feeStateId: number;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  items: FeeStructureItemRequest[];
  reason?: string;
}

export interface FeeStructure {
  id: number;
  groupId: number;
  programId: number;
  programName: string;
  courseId: number | null;
  courseName: string | null;
  academicYearId: number;
  academicYearName: string;
  quota: 'MANAGEMENT' | 'COUNSELLING';
  feeStateId: number;
  feeStateName: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
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
  groupId: number;
  programId: number;
  programName: string;
  courseId: number | null;
  courseName: string | null;
  academicYearId: number;
  academicYearName: string;
  quota: 'MANAGEMENT' | 'COUNSELLING';
  feeStateId: number;
  feeStateName: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
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
  collectibleNow: boolean;
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
  /** Bank transfer/DD only + FEE_COLLECT_EXCESS — allow amount above total outstanding. */
  allowExcess?: boolean;
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
  surplusAmount: number;
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

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface FeeExplorerParams {
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
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
  yearOfStudy?: number;
  academicYearName?: string;
  collectibleOutstanding: number;
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
  studentName: string | null;
  rollNumber: string | null;
  installmentFeeId: number | null;
  installmentLabel: string | null;
  yearNumber: number | null;
  amountPaid: number;
  paymentDate: string;
  paymentMode: string | null;
  transactionReference: string | null;
  remarks: string | null;
  createdAt: string;
  receiptType: 'PAYMENT' | 'ENQUIRY_PAYMENT' | 'REFUND';
  originalReceiptNumber: string | null;
  feeCategory: 'TUITION_ONLY' | 'TUITION_AND_HOSTEL' | null;
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
  academicYearId: number | null;
  academicYearName: string | null;
  amountPaid: number;
  paymentDate: string;
  paymentMode: string | null;
  transactionReference: string | null;
  remarks: string | null;
  installmentsCovered: string | null;
  collectedBy: string | null;
  feeCategory: 'TUITION_ONLY' | 'TUITION_AND_HOSTEL' | null;
  createdAt: string;
  /** PAYMENT = original receipt; REFUND = reversal record */
  receiptType: 'PAYMENT' | 'REFUND';
  /** True when this original payment receipt has already been refunded (APPROVED). */
  refunded: boolean;
  /** PENDING or APPROVED when refund flow is active for this original receipt; null otherwise. */
  refundStatus: 'PENDING' | 'APPROVED' | null;
}

export interface FeeRefundRequest {
  receiptNumber: string;
  reason: string;
}

/** Returned when a refund request is initiated (status always = PENDING). */
export interface FeeRefundResponse {
  id: number;
  originalReceiptNumber: string;
  refundAmount: number;
  reason: string;
  studentName: string;
  rollNumber: string | null;
  status: string;
}

/** One refund request shown in the approval list. */
export interface FeeRefundSummary {
  id: number;
  originalReceiptNumber: string;
  entityType: 'STUDENT' | 'ENQUIRY';
  studentName: string;
  rollNumber: string | null;
  admissionNumber: string | null;
  programName: string | null;
  academicYearId: number | null;
  academicYearName: string | null;
  refundAmount: number;
  reason: string;
  requestedBy: string | null;
  requestedAt: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'TRANSMITTED' | 'PAYMENT_FAILED';
  /** MANUAL (staff-initiated) | AUTO_EXCESS (system-generated, cannot be rejected). */
  source: 'MANUAL' | 'AUTO_EXCESS';
  // Populated after APPROVED
  refundNumber: string | null;
  paymentMode: string | null;
  paymentDate: string | null;
  transactionReference: string | null;
  approvedBy: string | null;
  approvedAt: string | null;
  // Populated after REJECTED
  rejectionReason: string | null;
}

export interface FeeRefundApprovalRequest {
  paymentMode: string;
  paymentDate: string;  // ISO date yyyy-MM-dd
  transactionReference?: string;
}

export interface FeeRefundRejectionRequest {
  rejectionReason: string;
}

export interface EnquiryCreditApplication {
  id: number;
  enquiryId: number;
  enquiryName: string;
  studentId: number;
  studentName: string;
  rollNumber: string;
  semesterFeeId: number;
  semesterLabel: string;
  amountApplied: number;
  receiptNumber: string;
  appliedAt: string;
}
