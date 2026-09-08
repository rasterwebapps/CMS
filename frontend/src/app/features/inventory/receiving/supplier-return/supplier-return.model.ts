export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type SupplierReturnStatus = 'DRAFT' | 'COMPLETED' | 'CANCELLED';
export type SupplierReturnReason = 'DEFECTIVE' | 'WRONG_ITEM' | 'DAMAGED_IN_TRANSIT' | 'QUALITY_ISSUE' | 'OTHER';

export interface SupplierReturnCreateRequest {
  goodsReceiptId: number;
  returnDate?: string;
  reason?: string;
  notes?: string;
}

export interface SupplierReturnAddLineRequest {
  goodsReceiptLineId: number;
  returnedQty: number;
  notes?: string;
}

export interface SupplierReturnLine {
  id: number;
  goodsReceiptLineId: number;
  productId: number;
  productCode: string;
  productName: string;
  uomCode: string | null;
  receivedQty: number;
  alreadyReturnedQty: number;
  returnedQty: number;
  notes: string | null;
}

export interface SupplierReturn {
  id: number;
  goodsReceiptId: number;
  supplierName: string;
  locationId: number;
  locationVirtualName: string;
  status: SupplierReturnStatus;
  reason: SupplierReturnReason | null;
  returnDate: string;
  notes: string | null;
  createdBy: string | null;
  createdAt: string;
  completedBy: string | null;
  completedAt: string | null;
  lineCount: number;
  lines: SupplierReturnLine[] | null;
}

/** A confirmed receipt line still holding a returnable quantity — the add-line picker's pool. */
export interface ReturnableGoodsReceiptLine {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  uomCode: string | null;
  receivedQty: number;
  alreadyReturnedQty: number;
  openQty: number;
}
