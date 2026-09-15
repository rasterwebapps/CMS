export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface InventoryRack {
  id: number;
  locationId: number;
  locationName?: string;
  name: string;
  code: string;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface InventoryRackRequest {
  locationId: number;
  name: string;
  code: string;
  description?: string;
  isActive?: boolean;
}

export interface InventoryBin {
  id: number;
  rackId: number;
  rackName?: string;
  locationId: number;
  locationName?: string;
  name: string;
  code: string;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface InventoryBinRequest {
  rackId: number;
  name: string;
  code: string;
  description?: string;
  isActive?: boolean;
}

export interface InventoryRackBinStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
