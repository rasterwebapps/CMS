/** Settable on Block, Floor, or Zone — whichever level matches the physical reality. Setting it
 *  (with isHostel) on a Block or Floor cascades the same value down to every level underneath. */
export type GenderRestriction = 'BOYS' | 'GIRLS';

export interface Organization {
  id: number;
  name: string;
  code: string;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface OrganizationRequest {
  name: string;
  code: string;
  description?: string;
  isActive?: boolean;
}

export interface Branch {
  id: number;
  name: string;
  code: string;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  organizationId: number;
  organizationName: string;
}

export interface BranchRequest {
  name: string;
  code: string;
  description?: string;
  isActive?: boolean;
  organizationId?: number | null;
}

export interface Block {
  id: number;
  name: string;
  code: string;
  description?: string;
  isHostel: boolean;
  genderRestriction: GenderRestriction | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  branchId: number;
  branchName: string;
}

export interface BlockRequest {
  name: string;
  code: string;
  description?: string;
  isHostel?: boolean;
  genderRestriction?: GenderRestriction | null;
  isActive?: boolean;
  branchId?: number | null;
}

export interface Floor {
  id: number;
  name: string;
  floorNumber: number;
  isHostel: boolean;
  genderRestriction: GenderRestriction | null;
  /** Explicit skyline (false, above ground) vs earthline (true, basement) marker for the Campus
   *  Setup diagram — independent of floorNumber's ordering role. */
  isBasement: boolean;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  blockId: number;
  blockName: string;
}

export interface FloorRequest {
  name: string;
  floorNumber: number;
  isHostel?: boolean;
  genderRestriction?: GenderRestriction | null;
  isBasement?: boolean;
  isActive?: boolean;
  blockId?: number | null;
}

export interface Zone {
  id: number;
  name: string;
  isHostel: boolean;
  genderRestriction: GenderRestriction | null;
  wardenId: number | null;
  wardenName: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  floorId: number;
  floorName: string;
}

export interface ZoneRequest {
  name: string;
  isHostel?: boolean;
  genderRestriction?: GenderRestriction | null;
  wardenId?: number | null;
  isActive?: boolean;
  floorId?: number | null;
}

export interface Room {
  id: number;
  roomNumber: string;
  capacity: number | null;
  description?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  zoneId: number;
  zoneName: string;
  hostelRoomId: number | null;
  hostelRoomTypeId: number | null;
  hostelRoomTypeName: string | null;
  purposeCategoryId: number | null;
  purposeCategoryName: string | null;
  subTypeId: number | null;
  subTypeName: string | null;
}

export interface RoomRequest {
  roomNumber: string;
  capacity?: number | null;
  description?: string;
  isActive?: boolean;
  zoneId?: number | null;
  purposeCategoryId?: number | null;
  subTypeId?: number | null;
}

export interface HostelRoom {
  id: number;
  roomId: number;
  roomNumber: string;
  zoneId: number;
  zoneName: string;
  roomTypeId: number;
  roomTypeName: string;
  sharingCapacity: number;
  isAc: boolean;
  feeAmountPerYear: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface HostelRoomRequest {
  roomTypeId: number;
  isActive?: boolean;
}

export interface CampusInfraStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface CampusInfraStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
