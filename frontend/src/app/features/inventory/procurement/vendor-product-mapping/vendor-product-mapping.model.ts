export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface VendorProductMapping {
  id: number;
  supplierId: number;
  supplierName: string;
  productId: number;
  productCode: string;
  productName: string;
  rateContractId: number | null;
  unitPrice: number;
  currencyCode: string;
  uomId: number | null;
  uomCode: string | null;
  minOrderQty: number | null;
  leadTimeDays: number | null;
  isPreferred: boolean;
  isActive: boolean;
  /** unitPrice, unless an active, in-window RateContract line for this product overrides it. */
  effectivePrice: number;
  /** 'CONTRACT' when effectivePrice came from a RateContractLine, 'STANDARD' otherwise. */
  priceSource: 'CONTRACT' | 'STANDARD';
  createdAt: string;
  updatedAt: string;
}

export interface VendorProductMappingStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface VendorProductMappingStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}

export interface VendorProductMappingRequest {
  supplierId: number;
  productId: number;
  rateContractId?: number;
  unitPrice: number;
  currencyCode?: string;
  uomId?: number;
  minOrderQty?: number;
  leadTimeDays?: number;
  isPreferred?: boolean;
  isActive?: boolean;
}
