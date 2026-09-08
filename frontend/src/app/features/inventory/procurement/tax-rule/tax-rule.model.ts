export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface TaxRule {
  id: number;
  name: string;
  ratePercent: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TaxRuleRequest {
  name: string;
  ratePercent: number;
  isActive?: boolean;
}

export interface TaxRuleStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface TaxRuleStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
