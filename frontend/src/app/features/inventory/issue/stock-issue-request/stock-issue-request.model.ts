export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type StockIssueRequestStatus = 'DRAFT' | 'SUBMITTED' | 'COMPLETED' | 'CANCELLED';
export type StockIssueRequestItemStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface StockIssueRequestCreateRequest {
  requestingLocationId: number;
  issuingLocationId: number;
  requestDate?: string;
  notes?: string;
}

export interface StockIssueRequestAddLineRequest {
  productId: number;
  requestedQty: number;
  notes?: string;
}

export interface StockIssueRequestResolutionRequest {
  notes?: string;
}

export interface StockIssueRequestItem {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  uomCode: string | null;
  requestedQty: number;
  status: StockIssueRequestItemStatus;
  resolvedBy: string | null;
  resolvedAt: string | null;
  resolutionNotes: string | null;
  notes: string | null;
}

export interface StockIssueRequest {
  id: number;
  requestingLocationId: number;
  requestingLocationVirtualName: string;
  issuingLocationId: number;
  issuingLocationVirtualName: string;
  status: StockIssueRequestStatus;
  requestDate: string;
  notes: string | null;
  createdBy: string | null;
  createdAt: string;
  submittedBy: string | null;
  submittedAt: string | null;
  completedAt: string | null;
  lineCount: number;
  pendingCount: number;
  lines: StockIssueRequestItem[] | null;
}
