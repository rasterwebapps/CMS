export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ProductLocationReorderConfig {
  id: number;
  productId: number;
  productCode: string;
  productName: string;
  locationId: number;
  locationVirtualName: string;
  /** The location's own default supplying store, shown here so a missing one is obvious before
   *  auto-indent is even attempted — set on the Inventory Location itself, not here. */
  defaultSupplyingLocationId: number | null;
  defaultSupplyingLocationVirtualName: string | null;
  reorderLevel: number;
  reorderQty: number;
  maxStockQty: number | null;
  autoIndentEnabled: boolean;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProductLocationReorderConfigRequest {
  productId: number;
  locationId: number;
  reorderLevel: number;
  reorderQty: number;
  maxStockQty?: number | null;
  autoIndentEnabled?: boolean;
  isActive?: boolean;
}

export interface ProductLocationReorderConfigStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface ProductLocationReorderConfigStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
