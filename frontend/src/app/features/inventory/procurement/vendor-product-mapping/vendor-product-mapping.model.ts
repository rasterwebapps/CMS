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
  /** The supplier's own SKU/part number for this product — commonly differs from ours. */
  vendorPartNumber: string | null;
  /** The supplier's own name for this product, if it differs from ours. */
  vendorProductName: string | null;
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
  /** The institution's configured base currency, or null if not configured yet. */
  baseCurrencyCode: string | null;
  /** effectivePrice converted to baseCurrencyCode, or null when unresolvable (no base currency
   *  configured, or no exchange rate on file for this mapping's currency). */
  effectivePriceInBaseCurrency: number | null;
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
  vendorPartNumber?: string;
  vendorProductName?: string;
  unitPrice: number;
  currencyCode?: string;
  uomId?: number;
  minOrderQty?: number;
  leadTimeDays?: number;
  isPreferred?: boolean;
  isActive?: boolean;
}
