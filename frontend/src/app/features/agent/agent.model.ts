export interface Agent {
  id: number;
  name: string;
  phone: string;
  email: string;
  area: string;
  locality: string;
  allottedSeats: number | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AgentRequest {
  name: string;
  phone?: string;
  email?: string;
  area?: string;
  locality?: string;
  allottedSeats?: number | null;
  isActive?: boolean;
}

export interface AgentCommissionGuideline {
  id: number;
  agentId: number;
  agentName: string;
  programId: number;
  programName: string;
  localityType: string;
  suggestedCommission: number;
  createdAt: string;
  updatedAt: string;
}

export interface AgentCommissionGuidelineRequest {
  agentId: number;
  programId: number;
  localityType: string;
  suggestedCommission: number;
}
