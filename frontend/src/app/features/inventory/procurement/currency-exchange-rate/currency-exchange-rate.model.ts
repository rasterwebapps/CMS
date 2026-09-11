export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface CurrencyExchangeRate {
  id: number;
  currencyCode: string;
  rateToBase: number;
  effectiveDate: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CurrencyExchangeRateRequest {
  currencyCode: string;
  rateToBase: number;
  effectiveDate: string;
  isActive?: boolean;
}

export interface CurrencyExchangeRateStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface CurrencyExchangeRateStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
