export type LocationRole = 'STORE' | 'REQUESTING_POINT' | 'BOTH';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface InventoryLocation {
  id: number;
  roomId: number;
  roomNumber: string;
  zoneId: number;
  zoneName: string;
  virtualName: string;
  locationRole: LocationRole;
  description?: string;
  defaultSupplyingLocationId?: number | null;
  defaultSupplyingLocationVirtualName?: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface InventoryLocationRequest {
  roomId: number;
  virtualName: string;
  locationRole: LocationRole;
  description?: string;
  defaultSupplyingLocationId?: number | null;
  isActive?: boolean;
}

export interface InventoryLocationStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface InventoryLocationStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
