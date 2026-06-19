export type BankAccountType = 'SAVINGS' | 'CURRENT' | 'SALARY';

export interface Agent {
  id: number;
  name: string;
  phone: string;
  email: string;
  area: string;
  locality: string;
  allottedSeats: number | null;
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

export interface AgentRequest {
  name: string;
  phone?: string;
  email?: string;
  area?: string;
  locality?: string;
  allottedSeats?: number | null;
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

export interface AgentStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface AgentStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}

export const AGENT_BANK_ACCOUNT_TYPE_OPTIONS: { value: BankAccountType; label: string }[] = [
  { value: 'SAVINGS', label: 'Savings' },
  { value: 'CURRENT', label: 'Current' },
  { value: 'SALARY', label: 'Salary' },
];

export interface AgentCommissionGuideline {
  id: number;
  agentId: number;
  agentName: string;
  programId: number;
  programName: string;
  localityType: string;
  suggestedCommission: number;
  createdAt: string;
  updatedAt: string;
}

export interface AgentCommissionGuidelineRequest {
  agentId: number;
  programId: number;
  localityType: string;
  suggestedCommission: number;
}
