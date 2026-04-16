export interface ReferralType {
  id: number;
  name: string;
  code: string;
  commissionAmount: number;
  hasCommission: boolean;
  description: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ReferralTypeRequest {
  name: string;
  code: string;
  commissionAmount: number;
  hasCommission: boolean;
  description?: string;
  isActive?: boolean;
}
