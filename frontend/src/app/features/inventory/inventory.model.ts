export interface InventoryItem {
  id: number;
  name: string;
  labId: number;
  labName: string;
  category: string;
  quantity: number;
  unit: string;
  minimumStockLevel: number;
  location?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface InventoryItemRequest {
  name: string;
  labId: number;
  category: string;
  quantity: number;
  unit: string;
  minimumStockLevel?: number;
  location?: string;
  notes?: string;
}
