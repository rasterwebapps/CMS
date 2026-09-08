export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type GatePassDirection = 'OUTWARD' | 'INWARD';

export type GatePassStatus = 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'GATE_VERIFIED' | 'RETURNED' | 'CLOSED';

export interface GatePassCreateRequest {
  direction: GatePassDirection;
  returnable: boolean;
  productId?: number | null;
  assetId?: number | null;
  locationId: number;
  quantity: number;
  reason: string;
  partyName: string;
  partyContact?: string;
  linkedPurchaseOrderId?: number | null;
  passDate?: string;
  expectedReturnDate?: string | null;
  notes?: string;
}

export interface GatePassRejectRequest {
  reason: string;
}

export interface GatePassReturnRequest {
  notes?: string;
}

export interface GatePass {
  id: number;
  direction: GatePassDirection;
  returnable: boolean;
  productId: number | null;
  productCode: string | null;
  productName: string | null;
  assetId: number | null;
  assetTag: string | null;
  locationId: number;
  locationVirtualName: string;
  quantity: number;
  reason: string;
  partyName: string;
  partyContact: string | null;
  linkedPurchaseOrderId: number | null;
  passDate: string;
  expectedReturnDate: string | null;
  actualReturnDate: string | null;
  overdue: boolean;
  status: GatePassStatus;
  notes: string | null;
  createdBy: string | null;
  createdAt: string;
  approvedBy: string | null;
  approvedAt: string | null;
  rejectedBy: string | null;
  rejectedAt: string | null;
  rejectionReason: string | null;
  gateVerifiedBy: string | null;
  gateVerifiedAt: string | null;
  returnedBy: string | null;
  returnedAt: string | null;
}
