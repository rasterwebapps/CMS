export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type ApprovalInstanceStatus = 'IN_PROGRESS' | 'APPROVED' | 'REJECTED';
export type ApprovalActionStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface ApprovalInstanceStartRequest {
  workflowId: number;
  purchaseRequisitionId?: number;
  purchaseOrderId?: number;
}

export interface ApprovalActionResolutionRequest {
  notes?: string;
}

export type ApprovalExceptionReason = 'URGENT_PURCHASE' | 'SINGLE_SUPPLIER_SITUATION' | 'EMERGENCY' | 'APPROVER_UNAVAILABLE' | 'OTHER';

export const APPROVAL_EXCEPTION_REASONS: { value: ApprovalExceptionReason; label: string }[] = [
  { value: 'URGENT_PURCHASE', label: 'Urgent Purchase' },
  { value: 'SINGLE_SUPPLIER_SITUATION', label: 'Single-Supplier Situation' },
  { value: 'EMERGENCY', label: 'Emergency' },
  { value: 'APPROVER_UNAVAILABLE', label: 'Approver Unavailable' },
  { value: 'OTHER', label: 'Other' },
];

export interface ApprovalActionBypassRequest {
  reason: ApprovalExceptionReason;
  notes?: string;
}

export interface ApprovalAction {
  id: number;
  stepOrder: number;
  stepName: string;
  requiredPermissionCode: string;
  requiredPermissionDisplayName: string;
  status: ApprovalActionStatus;
  actedBy: string | null;
  actedAt: string | null;
  notes: string | null;
  exceptionReason: ApprovalExceptionReason | null;
  actionableByCurrentUser: boolean;
  bypassableByCurrentUser: boolean;
}

export interface ApprovalInstance {
  id: number;
  workflowId: number;
  workflowName: string;
  documentType: string;
  purchaseRequisitionId: number | null;
  purchaseOrderId: number | null;
  status: ApprovalInstanceStatus;
  currentStepOrder: number;
  initiatedBy: string | null;
  initiatedAt: string;
  completedAt: string | null;
  actions: ApprovalAction[];
}
