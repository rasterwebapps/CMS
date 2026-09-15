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
  variantId: number | null;
  variantCode: string | null;
  variantName: string | null;
  /** True once the product has any active variant — flags a null variantId row as a stranded
   *  ("Unassigned") balance that predates the product's first variant, rather than a normal
   *  balance for a product that has never had variants. */
  productHasActiveVariants: boolean;
  locationId: number;
  locationVirtualName: string;
  batchId: number | null;
  batchOrSerialNo: string | null;
  expiryDate: string | null;
  qtyOnHand: number;
  valueOnHand: number;
  lastUpdated: string;
}

export interface StockBinAllocation {
  binId: number;
  binCode: string;
  binName: string;
  rackName: string;
  qty: number;
}

export interface StockBalanceBinBreakdown {
  balanceId: number;
  qtyOnHand: number;
  unallocatedQty: number;
  allocations: StockBinAllocation[];
}

export type StockTxnType = 'RECEIPT' | 'ADJUSTMENT' | 'DISPOSAL';
export type AdjustmentDirection = 'INCREASE' | 'DECREASE';

export interface StockMovementRequest {
  productId: number;
  /** Required once the product has any active ProductVariant. */
  variantId?: number;
  locationId: number;
  batchOrSerialNo?: string;
  expiryDate?: string;
  txnType: StockTxnType;
  direction?: AdjustmentDirection;
  quantity: number;
  unitCost?: number;
  notes?: string;
  /** Optional — the specific bin within locationId this movement's leg allocates against. */
  binId?: number;
}

export interface StockMovementResponse {
  ledgerId: number;
  productId: number;
  variantId: number | null;
  locationId: number;
  batchId: number | null;
  txnType: string;
  qtyDelta: number;
  newQtyOnHand: number;
  newValueOnHand: number;
  txnDate: string;
}

export interface VariantConvertRequest {
  variantId: number;
}
