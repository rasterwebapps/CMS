import { ProductAttributeValueEntry, ProductAttributeValueRequest, StockTrackingMode } from '../product.model';

export interface ProductVariant {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  variantCode: string;
  variantName: string;
  barcode?: string | null;
  trackingMode: StockTrackingMode;
  standardCost?: number | null;
  listPrice?: number | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  /** Reuses the same typed-attribute-value shape as Product's own — see product.model.ts. */
  attributeValues: ProductAttributeValueEntry[];
}

export interface ProductVariantRequest {
  variantCode: string;
  variantName: string;
  barcode?: string | null;
  trackingMode?: StockTrackingMode;
  standardCost?: number | null;
  listPrice?: number | null;
  isActive?: boolean;
  attributeValues?: ProductAttributeValueRequest[];
}

export interface ProductVariantStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface ProductVariantStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
