export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type CycleCountScope = 'FULL_LOCATION' | 'AD_HOC';
export type CycleCountStatus = 'DRAFT' | 'SUBMITTED' | 'COMPLETED' | 'CANCELLED';
export type CycleCountLineStatus = 'PENDING_COUNT' | 'MATCHED' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED';

export interface CycleCountCreateRequest {
  locationId: number;
  countDate?: string;
  scope: CycleCountScope;
  notes?: string;
}

export interface CycleCountAddLineRequest {
  productId: number;
  notes?: string;
}

export interface CycleCountEnterCountRequest {
  countedQty: number;
  notes?: string;
}

export interface CycleCountResolutionRequest {
  notes?: string;
}

/**
 * systemQtySnapshot and varianceQty come back null while the count is DRAFT — the blind-count
 * design deliberately withholds them from the entry screen; they're only populated once the
 * count is submitted. See docs/inventory-management/DECISION_LOG.md's 2026-09-08 entry.
 */
export interface CycleCountLine {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  uomCode: string | null;
  systemQtySnapshot: number | null;
  countedQty: number | null;
  varianceQty: number | null;
  status: CycleCountLineStatus;
  countedBy: string | null;
  countedAt: string | null;
  resolvedBy: string | null;
  resolvedAt: string | null;
  resolutionNotes: string | null;
  ledgerRefId: number | null;
  notes: string | null;
}

export interface CycleCount {
  id: number;
  locationId: number;
  locationVirtualName: string;
  scope: CycleCountScope;
  status: CycleCountStatus;
  countDate: string;
  notes: string | null;
  createdBy: string | null;
  createdAt: string;
  submittedBy: string | null;
  submittedAt: string | null;
  completedAt: string | null;
  lineCount: number;
  pendingReviewCount: number;
  lines: CycleCountLine[] | null;
}
