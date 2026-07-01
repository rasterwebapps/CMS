export type BankAccountType = 'SAVINGS' | 'CURRENT' | 'SALARY';

export interface StaffReferrer {
  id: number;
  name: string;
  phone: string | null;
  email: string | null;
  employeeCode: string;
  institutionId: number;
  institutionName: string;
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
  employeeCode: string;
  institutionId: number;
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

export interface StaffReferrerStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface StaffReferrerStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}

export const STAFF_REFERRER_BANK_ACCOUNT_TYPE_OPTIONS: { value: BankAccountType; label: string }[] = [
  { value: 'SAVINGS', label: 'Savings' },
  { value: 'CURRENT', label: 'Current' },
  { value: 'SALARY', label: 'Salary' },
];

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
