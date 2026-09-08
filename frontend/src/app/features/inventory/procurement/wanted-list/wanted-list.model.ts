export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type WantedListItemStatus = 'PENDING' | 'DEFERRED' | 'REJECTED' | 'CONVERTED';
export type WantedListRejectionReason = 'ALREADY_ORDERED_ELSEWHERE' | 'PRODUCT_DISCONTINUING' | 'LEVEL_MISCALIBRATED' | 'OTHER';

export const WANTED_LIST_REJECTION_REASONS: { value: WantedListRejectionReason; label: string }[] = [
  { value: 'ALREADY_ORDERED_ELSEWHERE', label: 'Already ordered elsewhere' },
  { value: 'PRODUCT_DISCONTINUING', label: 'Product is being discontinued' },
  { value: 'LEVEL_MISCALIBRATED', label: 'Reorder level needs recalibrating' },
  { value: 'OTHER', label: 'Other' },
];

export interface WantedListItem {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  uomCode: string | null;
  locationId: number;
  locationVirtualName: string;
  status: WantedListItemStatus;
  qtyOnHandSnapshot: number;
  qtyOnOrderSnapshot: number;
  reorderLevelSnapshot: number;
  suggestedQty: number;
  generatedAt: string;
  resolvedBy: string | null;
  resolvedAt: string | null;
  resolutionNotes: string | null;
  rejectionReason: WantedListRejectionReason | null;
  convertedPurchaseRequisitionId: number | null;
  convertedPurchaseRequisitionItemId: number | null;
}

export interface WantedListResolutionRequest {
  notes?: string;
}

export interface WantedListRejectRequest {
  reason: WantedListRejectionReason;
  notes?: string;
}

export interface WantedListConvertRequest {
  lines: { itemId: number; qty?: number }[];
  requisitionDate?: string;
  notes?: string;
}
