export type DemandStatus = 'UNPAID' | 'PARTIAL' | 'PAID' | 'WAIVED';

export interface FeeDemandReport {
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

export interface FeeCollectionSummary {
  programName: string;
  programCode: string;
  totalDemands: number;
  totalAmount: number;
  collectedAmount: number;
  outstandingAmount: number;
  paidCount: number;
  partialCount: number;
  unpaidCount: number;
}

export interface TermFeePaymentReport {
  id: number;
  feeDemandId: number;
  studentName: string;
  paymentDate: string;
  amountPaid: number;
  lateFeeApplied: number;
  totalCollected: number;
  paymentMode: string;
  receiptNumber: string;
  remarks?: string;
  demandStatus: DemandStatus;
}

export interface StudentLedgerEntry {
  demandId: number;
  termLabel: string;
  totalAmount: number;
  paidAmount: number;
  outstandingAmount: number;
  dueDate: string;
  status: DemandStatus;
  payments: TermFeePaymentReport[];
}

export interface StudentFeeLedgerReport {
  studentId: number;
  studentName: string;
  entries: StudentLedgerEntry[];
}
