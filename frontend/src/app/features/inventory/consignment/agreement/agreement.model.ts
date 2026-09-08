export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ConsignmentAgreementRequest {
  supplierId: number;
  locationId: number;
  agreementNumber: string;
  startDate: string;
  endDate?: string | null;
  billingCycleDays?: number | null;
  notes?: string;
  isActive?: boolean;
}

export interface ConsignmentAgreement {
  id: number;
  supplierId: number;
  supplierName: string;
  locationId: number;
  locationVirtualName: string;
  agreementNumber: string;
  startDate: string;
  endDate: string | null;
  billingCycleDays: number | null;
  notes: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}
