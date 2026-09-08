export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface StockBalance {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  locationId: number;
  locationVirtualName: string;
  batchId: number | null;
  batchOrSerialNo: string | null;
  expiryDate: string | null;
  qtyOnHand: number;
  valueOnHand: number;
  lastUpdated: string;
}

export type StockTxnType = 'RECEIPT' | 'ADJUSTMENT' | 'DISPOSAL';
export type AdjustmentDirection = 'INCREASE' | 'DECREASE';

export interface StockMovementRequest {
  productId: number;
  locationId: number;
  batchOrSerialNo?: string;
  expiryDate?: string;
  txnType: StockTxnType;
  direction?: AdjustmentDirection;
  quantity: number;
  unitCost?: number;
  notes?: string;
}

export interface StockMovementResponse {
  ledgerId: number;
  productId: number;
  locationId: number;
  batchId: number | null;
  txnType: string;
  qtyDelta: number;
  newQtyOnHand: number;
  newValueOnHand: number;
  txnDate: string;
}
