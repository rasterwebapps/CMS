export interface Institution {
  id: number;
  name: string;
  code: string;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface InstitutionRequest {
  name: string;
  code: string;
  description?: string;
  isActive?: boolean;
}

export interface InstitutionStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface InstitutionStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
