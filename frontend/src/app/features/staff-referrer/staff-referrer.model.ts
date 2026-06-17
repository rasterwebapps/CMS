export type BankAccountType = 'SAVINGS' | 'CURRENT' | 'SALARY';

export interface StaffReferrer {
  id: number;
  name: string;
  phone: string | null;
  email: string | null;
  institution: string | null;
  commissionAmount: number | null;
  isActive: boolean;
  panNumber?: string;
  aadhaarNumber?: string;
  bankAccountNumber?: string;
  bankIfscCode?: string;
  bankBranch?: string;
  bankName?: string;
  bankAccountHolder?: string;
  bankAccountType?: BankAccountType;
  createdAt: string;
  updatedAt: string;
}

export interface StaffReferrerRequest {
  name: string;
  phone?: string;
  email?: string;
  institution?: string;
  commissionAmount?: number | null;
  isActive?: boolean;
  panNumber?: string;
  aadhaarNumber?: string;
  bankAccountNumber?: string;
  bankIfscCode?: string;
  bankBranch?: string;
  bankName?: string;
  bankAccountHolder?: string;
  bankAccountType?: BankAccountType;
}

export const STAFF_REFERRER_BANK_ACCOUNT_TYPE_OPTIONS: { value: BankAccountType; label: string }[] = [
  { value: 'SAVINGS', label: 'Savings' },
  { value: 'CURRENT', label: 'Current' },
  { value: 'SALARY', label: 'Salary' },
];
