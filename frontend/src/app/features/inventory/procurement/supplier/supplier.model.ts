export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

/**
 * taxRegistrationId/legalRegistrationNo/bankAccountNumber come back masked to their last 4
 * characters (e.g. "••••1234") unless the caller holds INVENTORY_SUPPLIER_MANAGE — bankMasked
 * tells the form/detail view that masking was applied, so it doesn't render a masked value as if
 * it were empty.
 */
export interface Supplier {
  id: number;
  supplierCode: string;
  supplierName: string;
  taxRegistrationId: string | null;
  legalRegistrationNo: string | null;
  bankAccountNumber: string | null;
  bankIfscCode: string | null;
  bankName: string | null;
  bankAccountHolder: string | null;
  bankMasked: boolean;
  contactPerson: string | null;
  email: string | null;
  phone: string | null;
  isApproved: boolean;
  approvalDate: string | null;
  portalAccessEnabled: boolean;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SupplierRequest {
  supplierCode: string;
  supplierName: string;
  taxRegistrationId?: string;
  legalRegistrationNo?: string;
  bankAccountNumber?: string;
  bankIfscCode?: string;
  bankName?: string;
  bankAccountHolder?: string;
  contactPerson?: string;
  email?: string;
  phone?: string;
  portalAccessEnabled?: boolean;
  isActive?: boolean;
}

export interface SupplierStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface SupplierStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
