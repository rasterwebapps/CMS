export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type GoodsReceiptStatus = 'DRAFT' | 'CONFIRMED';

export interface GoodsReceiptCreateRequest {
  purchaseOrderId: number;
  receiptDate?: string;
  notes?: string;
}

export interface GoodsReceiptAddLineRequest {
  purchaseOrderItemId: number;
  receivedQty: number;
  unitCost?: number;
  batchOrSerialNo?: string;
  expiryDate?: string;
  notes?: string;
}

export interface GoodsReceiptLine {
  id: number;
  purchaseOrderItemId: number;
  productId: number;
  productCode: string;
  productName: string;
  uomCode: string | null;
  orderedQty: number;
  alreadyReceivedQty: number;
  receivedQty: number;
  unitCost: number | null;
  batchOrSerialNo: string | null;
  expiryDate: string | null;
  notes: string | null;
}

export interface GoodsReceipt {
  id: number;
  purchaseOrderId: number;
  supplierName: string;
  locationId: number;
  locationVirtualName: string;
  status: GoodsReceiptStatus;
  receiptDate: string;
  notes: string | null;
  createdBy: string | null;
  createdAt: string;
  confirmedBy: string | null;
  confirmedAt: string | null;
  lineCount: number;
  lines: GoodsReceiptLine[] | null;
}

/** A PO line still open to receive against — the add-line picker's pool. */
export interface ReceivablePurchaseOrderLine {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  uomCode: string | null;
  orderedQty: number;
  receivedQty: number;
  openQty: number;
  unitPrice: number;
}
