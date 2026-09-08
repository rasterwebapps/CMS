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
  actionableByCurrentUser: boolean;
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
