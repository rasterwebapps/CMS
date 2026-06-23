export interface CommissionPayout {
  id: number;
  amount: number;
  payoutDate: string;
  paymentMode: string;
  transactionReference: string | null;
  remarks: string | null;
  paidBy: string | null;
  createdAt: string;
}

export interface CommissionRecord {
  enquiryId: number;
  studentName: string;
  admissionNumber: string | null;
  enquiryStatus: string;
  programName: string | null;
  courseName: string | null;
  enquiryDate: string;

  referralTypeId: number | null;
  referralTypeName: string | null;
  commissionSource: string | null;

  agentId: number | null;
  agentName: string | null;
  staffReferrerId: number | null;
  staffReferrerName: string | null;
  referredFacultyId: number | null;
  referredFacultyName: string | null;

  commissionAmount: number;
  commissionPaidAmount: number;
  commissionOutstanding: number;
  commissionPaymentStatus: string;

  payouts: CommissionPayout[];

  // OneBook tracking (null when OneBook is not involved)
  oneBookReferenceId: string | null;
  oneBookStatus: string | null;
  oneBookTransmittedAt: string | null;
  oneBookTxnId: string | null;

  // Rejection tracking (null unless commissionPaymentStatus = REJECTED)
  rejectionReason: string | null;
  rejectedBy: string | null;
  rejectedAt: string | null;
}

export interface CommissionPayoutRequest {
  amount: number;
  payoutDate: string;
  paymentMode: string;
  transactionReference?: string;
  remarks?: string;
}

export const COMMISSION_STATUS_OPTIONS = [
  { value: 'PENDING',            label: 'Pending' },
  { value: 'PAYMENT_REQUESTED',  label: 'Awaiting Payment' },
  { value: 'PARTIAL',            label: 'Partial' },
  { value: 'PAID',               label: 'Paid' },
  { value: 'TRANSMITTED',        label: 'Transmitted' },
  { value: 'PROCESSING',         label: 'Processing' },
  { value: 'FAILED',             label: 'Failed' },
  { value: 'REJECTED',           label: 'Rejected' },
] as const;

export const COMMISSION_SOURCE_OPTIONS = [
  { value: 'AGENT',              label: 'Agent' },
  { value: 'STAFF_REFERRER',     label: 'Staff Referrer' },
  { value: 'FACULTY_REFERRER',   label: 'Faculty Referrer' },
  { value: 'REFERRAL_TYPE',      label: 'Referral Type' },
] as const;
