export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ReferralType {
  id: number;
  name: string;
  code: string;
  commissionAmount: number;
  hasCommission: boolean;
  description: string;
  isActive: boolean;
  isSystemDefined: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ReferralTypeRequest {
  name: string;
  code: string;
  commissionAmount: number;
  hasCommission: boolean;
  description?: string;
  isActive?: boolean;
}

export interface ReferralTypeStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface ReferralTypeStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}

