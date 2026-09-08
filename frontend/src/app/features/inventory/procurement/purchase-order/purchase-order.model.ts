export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type PurchaseOrderStatus = 'PENDING' | 'ORDERED' | 'IN_PROGRESS' | 'PARTIALLY_COMPLETED' | 'COMPLETED' | 'FORCE_CLOSED';

export interface PurchaseOrderCreateRequest {
  supplierId: number;
  locationId: number;
  poDate?: string;
  expectedDeliveryDate?: string;
  currencyCode?: string;
  exchangeRate?: number;
  notes?: string;
}

export interface PurchaseOrderAddLineRequest {
  purchaseRequisitionItemId: number;
  orderedQty?: number;
  unitPrice?: number;
  taxRuleId?: number;
}

export interface PurchaseOrderForceCloseRequest {
  reason: string;
}

export interface PurchaseOrderItem {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  uomCode: string | null;
  purchaseRequisitionItemId: number | null;
  orderedQty: number;
  unitPrice: number;
  taxRuleId: number | null;
  taxRuleName: string | null;
  taxAmount: number;
  lineTotal: number;
  receivedQty: number;
}

export interface PurchaseOrder {
  id: number;
  supplierId: number;
  supplierName: string;
  locationId: number;
  locationVirtualName: string;
  status: PurchaseOrderStatus;
  poDate: string;
  expectedDeliveryDate: string | null;
  currencyCode: string;
  exchangeRate: number | null;
  notes: string | null;
  createdBy: string | null;
  createdAt: string;
  orderedBy: string | null;
  orderedAt: string | null;
  forceClosedBy: string | null;
  forceClosedAt: string | null;
  forceCloseReason: string | null;
  lineCount: number;
  totalAmount: number;
  lines: PurchaseOrderItem[] | null;
}

/** A requisition line available to pick up into a PO — reuses the Purchase Requisition item shape. */
export interface AvailableRequisitionLine {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  uomCode: string | null;
  requestedQty: number;
  status: string;
  resolvedBy: string | null;
  resolvedAt: string | null;
  resolutionNotes: string | null;
  notes: string | null;
}
