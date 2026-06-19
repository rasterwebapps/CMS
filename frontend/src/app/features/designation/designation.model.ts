export interface DesignationMaster {
  id: number;
  name: string;
  code: string;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface DesignationRequest {
  name: string;
  code: string;
  description?: string;
  isActive?: boolean;
}

export interface DesignationStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface DesignationStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
