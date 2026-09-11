export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type StockTransferStatus = 'DRAFT' | 'COMPLETED' | 'CANCELLED';

export interface StockTransferCreateRequest {
  sourceLocationId: number;
  destinationLocationId: number;
  transferDate?: string;
  notes?: string;
}

export interface StockTransferAddLineRequest {
  productId: number;
  /** Required once the product has any active ProductVariant. */
  variantId?: number;
  quantity: number;
  notes?: string;
}

export interface StockTransferLine {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  variantId: number | null;
  variantCode: string | null;
  variantName: string | null;
  uomCode: string | null;
  quantity: number;
  notes: string | null;
}

export interface StockTransfer {
  id: number;
  sourceLocationId: number;
  sourceLocationVirtualName: string;
  destinationLocationId: number;
  destinationLocationVirtualName: string;
  status: StockTransferStatus;
  transferDate: string;
  notes: string | null;
  createdBy: string | null;
  createdAt: string;
  completedBy: string | null;
  completedAt: string | null;
  lineCount: number;
  lines: StockTransferLine[] | null;
}
