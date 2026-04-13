export interface MaintenanceRequest {
  id: number;
  equipmentId: number;
  equipmentName: string;
  requestedBy: string;
  description: string;
  priority: string;
  status: string;
  assignedTechnician?: string;
  repairCost?: number;
  completedDate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface MaintenanceRequestDto {
  equipmentId: number;
  requestedBy: string;
  description: string;
  priority: string;
  status?: string;
  assignedTechnician?: string;
  repairCost?: number;
  completedDate?: string;
}
