export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type PurchaseRequisitionStatus = 'DRAFT' | 'SUBMITTED' | 'COMPLETED' | 'CANCELLED';
export type PurchaseRequisitionItemStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface PurchaseRequisitionCreateRequest {
  locationId: number;
  requisitionDate?: string;
  notes?: string;
}

export interface PurchaseRequisitionAddLineRequest {
  productId: number;
  requestedQty: number;
  notes?: string;
}

export interface PurchaseRequisitionResolutionRequest {
  notes?: string;
}

export interface PurchaseRequisitionItem {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  uomCode: string | null;
  requestedQty: number;
  status: PurchaseRequisitionItemStatus;
  resolvedBy: string | null;
  resolvedAt: string | null;
  resolutionNotes: string | null;
  notes: string | null;
}

export interface PurchaseRequisition {
  id: number;
  locationId: number;
  locationVirtualName: string;
  status: PurchaseRequisitionStatus;
  requisitionDate: string;
  notes: string | null;
  createdBy: string | null;
  createdAt: string;
  submittedBy: string | null;
  submittedAt: string | null;
  completedAt: string | null;
  lineCount: number;
  pendingCount: number;
  lines: PurchaseRequisitionItem[] | null;
}
