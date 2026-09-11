import { AttributeDataType } from '../category/category-attribute.model';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

/** How a product's stock is tracked at the batch/serial level. NONE is the default — no batch or
 *  serial number is required, matching today's plain aggregate-quantity behavior. */
export type StockTrackingMode = 'NONE' | 'BATCH' | 'SERIAL';

export interface ProductAttributeValueEntry {
  attributeId: number;
  attributeName: string;
  dataType: AttributeDataType;
  value: string | null;
}

export interface Product {
  id: number;
  productCode: string;
  productName: string;
  categoryId: number;
  categoryName: string;
  baseUomId: number;
  baseUomCode: string;
  baseUomName: string;
  brandId?: number | null;
  brandName?: string | null;
  reorderLevel?: number | null;
  reorderQty?: number | null;
  isAsset: boolean;
  isConsumable: boolean;
  isService: boolean;
  isLoanable: boolean;
  trackingMode: StockTrackingMode;
  depreciationRate?: number | null;
  warrantyPeriodMonths?: number | null;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  aliases: string[];
  attributeValues: ProductAttributeValueEntry[];
}

export interface ProductAttributeValueRequest {
  attributeId: number;
  value?: string | null;
}

export interface ProductRequest {
  productCode: string;
  productName: string;
  categoryId: number;
  baseUomId: number;
  brandId?: number | null;
  reorderLevel?: number | null;
  reorderQty?: number | null;
  isAsset?: boolean;
  isConsumable?: boolean;
  isService?: boolean;
  isLoanable?: boolean;
  trackingMode?: StockTrackingMode;
  depreciationRate?: number | null;
  warrantyPeriodMonths?: number | null;
  description?: string;
  isActive?: boolean;
  aliases?: string[];
  attributeValues?: ProductAttributeValueRequest[];
}

export interface ProductStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface ProductStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}

/** One level of a product's unit-of-measure chain (e.g. "Strip", factor 10 -> the base unit).
 * levelRank 0 is always the product's base unit (factorToBase 1). factorToBase multiplies
 * directly to the base unit, not to the next level down. */
export interface ProductUomLevel {
  id: number;
  uomId: number;
  uomCode: string;
  uomName: string;
  levelRank: number;
  factorToBase: number;
  isDefaultPurchase: boolean;
}

export interface ProductUomChainVersion {
  id: number;
  versionNo: number;
  isActive: boolean;
  createdBy?: string | null;
  createdAt: string;
  levels: ProductUomLevel[];
}

export interface ProductUomLevelRequest {
  uomId: number;
  levelRank: number;
  factorToBase: number;
  isDefaultPurchase?: boolean;
}

export interface ProductUomChainSaveRequest {
  levels: ProductUomLevelRequest[];
}
