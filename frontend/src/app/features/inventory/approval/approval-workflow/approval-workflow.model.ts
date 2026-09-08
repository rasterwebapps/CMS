export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type ApprovalDocumentType = 'PURCHASE_REQUISITION' | 'PURCHASE_ORDER';

export interface ApprovalWorkflowStepRequest {
  stepOrder: number;
  stepName: string;
  permissionCode: string;
}

export interface ApprovalWorkflowRequest {
  name: string;
  documentType: ApprovalDocumentType;
  locationId?: number;
  minAmount?: number;
  isActive?: boolean;
  steps: ApprovalWorkflowStepRequest[];
}

export interface ApprovalWorkflowStep {
  id: number;
  stepOrder: number;
  stepName: string;
  permissionId: number;
  permissionCode: string;
  permissionDisplayName: string;
}

export interface ApprovalWorkflow {
  id: number;
  name: string;
  documentType: ApprovalDocumentType;
  locationId: number | null;
  locationVirtualName: string | null;
  minAmount: number | null;
  isActive: boolean;
  steps: ApprovalWorkflowStep[];
  createdAt: string;
  updatedAt: string;
}
