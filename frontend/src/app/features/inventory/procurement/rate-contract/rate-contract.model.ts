export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface RateContract {
  id: number;
  supplierId: number;
  supplierName: string;
  startDate: string;
  endDate: string | null;
  contractValueCap: number | null;
  termsText: string | null;
  renewalReminderDate: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface RateContractRequest {
  supplierId: number;
  startDate: string;
  endDate?: string;
  contractValueCap?: number;
  termsText?: string;
  renewalReminderDate?: string;
  isActive?: boolean;
}

export interface RateContractStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface RateContractStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
