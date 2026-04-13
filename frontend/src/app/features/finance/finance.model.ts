export interface FeeStructure {
  id: number;
  name: string;
  programId: number;
  programName: string;
  semester: number;
  amount: number;
  labFeeComponent: number;
  dueDate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface FeeStructureRequest {
  name: string;
  programId: number;
  semester: number;
  amount: number;
  labFeeComponent?: number;
  dueDate?: string;
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
