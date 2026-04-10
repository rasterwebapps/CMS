export interface Department {
  id: number;
  name: string;
  code: string;
  description?: string;
  hodName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface DepartmentRequest {
  name: string;
  code: string;
  description?: string;
  hodName?: string;
}
