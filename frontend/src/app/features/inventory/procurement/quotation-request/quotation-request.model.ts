export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type QuotationRequestStatus = 'DRAFT' | 'SUBMITTED' | 'COMPLETED' | 'CANCELLED';
export type QuotationRequestLineStatus = 'PENDING' | 'AWARDED' | 'REJECTED' | 'ORDERED';

export interface QuotationRequestCreateRequest {
  locationId: number;
  requestDate?: string;
  notes?: string;
}

export interface QuotationRequestAddLineRequest {
  purchaseRequisitionItemId: number;
  requestedQty?: number;
}

export interface QuotationRequestAddSupplierRequest {
  supplierId: number;
}

export interface QuotationResponseLineRequest {
  quotedUnitPrice: number;
  quotedLeadTimeDays?: number;
  notes?: string;
}

export interface QuotationRequestAwardRequest {
  responseLineId: number;
}

export interface QuotationResponseLine {
  id: number;
  supplierId: number;
  supplierName: string;
  quotedUnitPrice: number;
  quotedLeadTimeDays: number | null;
  notes: string | null;
  recordedBy: string | null;
  recordedAt: string;
}

export interface QuotationRequestLine {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  uomCode: string | null;
  purchaseRequisitionItemId: number;
  requestedQty: number;
  status: QuotationRequestLineStatus;
  awardedResponseLineId: number | null;
  awardedSupplierId: number | null;
  awardedSupplierName: string | null;
  awardedUnitPrice: number | null;
  awardedBy: string | null;
  awardedAt: string | null;
  responses: QuotationResponseLine[];
}

export interface QuotationRequestSupplier {
  id: number;
  supplierId: number;
  supplierName: string;
  invitedAt: string;
}

export interface QuotationRequest {
  id: number;
  locationId: number;
  locationVirtualName: string;
  status: QuotationRequestStatus;
  requestDate: string;
  notes: string | null;
  createdBy: string | null;
  createdAt: string;
  submittedBy: string | null;
  submittedAt: string | null;
  completedAt: string | null;
  lineCount: number;
  pendingCount: number;
  suppliers: QuotationRequestSupplier[] | null;
  lines: QuotationRequestLine[] | null;
}

/** The picker pool for "add a line" — an APPROVED requisition line not yet consumed elsewhere. */
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
