export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ConsignmentStockReceiveRequest {
  agreementId: number;
  productId: number;
  quantity: number;
  consignmentPrice: number;
  notes?: string;
}

export interface ConsignmentStockConsumeRequest {
  quantity: number;
  notes?: string;
}

export interface ConsignmentStockLine {
  id: number;
  agreementId: number;
  agreementNumber: string;
  supplierId: number;
  supplierName: string;
  locationId: number;
  locationVirtualName: string;
  productId: number;
  productCode: string;
  productName: string;
  consignmentPrice: number;
  receivedQty: number;
  consumedQty: number;
  qtyOnHand: number;
  lastReceivedBy: string | null;
  lastReceivedAt: string | null;
  lastConsumedBy: string | null;
  lastConsumedAt: string | null;
  createdAt: string;
  updatedAt: string;
}
