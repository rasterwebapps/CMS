export interface DesignationMaster {
  id: number;
  name: string;
  code: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface DesignationRequest {
  name: string;
  code: string;
  description?: string;
}
