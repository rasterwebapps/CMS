export interface Country {
  id: number;
  name: string;
  isoCode: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CountryRequest {
  name: string;
  isoCode: string;
  isActive?: boolean;
}

export interface IndiaState {
  id: number;
  name: string;
  code: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  countryId?: number;
  countryName?: string;
  countryIsoCode?: string;
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
  countryId?: number;
}

export interface IndiaDistrictRequest {
  stateId: number;
  name: string;
  isActive?: boolean;
}

