export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface BudgetRequest {
  locationId: number;
  periodStartDate: string;
  periodEndDate: string;
  allocatedAmount: number;
  notes?: string;
  isActive?: boolean;
}

export interface Budget {
  id: number;
  locationId: number;
  locationVirtualName: string;
  periodStartDate: string;
  periodEndDate: string;
  allocatedAmount: number;
  consumedAmount: number;
  remainingAmount: number;
  overAllocated: boolean;
  notes: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}
