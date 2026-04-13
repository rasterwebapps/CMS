export interface Equipment {
  id: number;
  name: string;
  model?: string;
  serialNumber?: string;
  labId: number;
  labName: string;
  category: string;
  status: string;
  purchaseDate?: string;
  purchaseCost?: number;
  warrantyExpiry?: string;
  createdAt: string;
  updatedAt: string;
}

export interface EquipmentRequest {
  name: string;
  model?: string;
  serialNumber?: string;
  labId: number;
  category: string;
  status?: string;
  purchaseDate?: string;
  purchaseCost?: number;
  warrantyExpiry?: string;
}
