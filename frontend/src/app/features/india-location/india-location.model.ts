export interface IndiaState {
  id: number;
  name: string;
  code: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface IndiaDistrict {
  id: number;
  stateId: number;
  stateName: string;
  name: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface IndiaStateRequest {
  name: string;
  code: string;
  isActive?: boolean;
}

export interface IndiaDistrictRequest {
  stateId: number;
  name: string;
  isActive?: boolean;
}

