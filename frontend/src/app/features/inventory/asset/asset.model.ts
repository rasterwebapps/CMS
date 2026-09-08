export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type AssetStatus = 'AVAILABLE' | 'IN_USE' | 'UNDER_MAINTENANCE' | 'RETIRED' | 'DISPOSED';

export interface AssetRequest {
  productId: number;
  locationId: number;
  assetTag: string;
  serialNumber?: string;
  goodsReceiptLineId?: number;
  purchaseValue?: number;
  purchaseDate?: string;
  usefulLifeMonths?: number;
  salvageValue?: number;
  notes?: string;
}

export interface AssetStatusUpdateRequest {
  status: AssetStatus;
  notes?: string;
}

export interface Asset {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  locationId: number;
  locationVirtualName: string;
  assetTag: string;
  serialNumber: string | null;
  status: AssetStatus;
  goodsReceiptLineId: number | null;
  purchaseValue: number | null;
  purchaseDate: string | null;
  usefulLifeMonths: number | null;
  salvageValue: number | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}
