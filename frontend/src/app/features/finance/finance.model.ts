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
  semesterFees: SemesterFeeDetail[];
  createdAt: string;
  updatedAt: string;
}

export interface SemesterFeeDetail {
  id: number;
  yearNumber: number;
  semesterLabel: string;
  amount: number;
  dueDate: string;
  amountPaid: number;
  pendingAmount: number;
  penaltyAmount: number;
  paymentStatus: string;
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
  semesterFeeId: number;
  semesterLabel: string;
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

export interface Receipt {
  id: number;
  receiptNumber: string;
  studentId: number;
  studentName: string;
  rollNumber: string;
  semesterFeeId: number;
  semesterLabel: string;
  yearNumber: number;
  amountPaid: number;
  paymentDate: string;
  paymentMode: string;
  transactionReference: string;
  remarks: string;
  createdAt: string;
}
