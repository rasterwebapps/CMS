export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface AssetServiceContractRequest {
  assetId: number;
  supplierId: number;
  contractNumber?: string;
  startDate: string;
  endDate?: string;
  renewalReminderDate?: string;
  coverageDetails?: string;
  isActive?: boolean;
}

export interface AssetServiceContract {
  id: number;
  assetId: number;
  assetTag: string;
  supplierId: number;
  supplierName: string;
  contractNumber: string | null;
  startDate: string;
  endDate: string | null;
  renewalReminderDate: string | null;
  expired: boolean;
  coverageDetails: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}
