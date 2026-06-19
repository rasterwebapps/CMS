export interface BloodGroup {
  id: number;
  name: string;
  code: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface BloodGroupRequest {
  name: string;
  code: string;
  isActive?: boolean;
}

export interface BloodGroupStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface BloodGroupStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}

