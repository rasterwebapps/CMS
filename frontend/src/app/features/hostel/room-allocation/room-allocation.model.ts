export type RoomAllocationStatus = 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface RoomAllocation {
  id: number;
  studentId: number;
  studentName: string;
  hostelRoomId: number;
  roomId: number;
  roomNumber: string;
  zoneId: number;
  zoneName: string;
  roomTypeId: number;
  roomTypeName: string;
  startDate: string;
  endDate: string | null;
  status: RoomAllocationStatus;
  remarks?: string;
  createdAt: string;
  updatedAt: string;
}

export interface RoomAllocationRequest {
  studentId: number;
  hostelRoomId: number;
  startDate: string;
  endDate?: string | null;
  status?: RoomAllocationStatus;
  remarks?: string;
}

export interface RoomAllocationOccupant {
  allocationId: number;
  studentId: number;
  studentName: string;
  startDate: string;
}

export interface HostelRoomOccupancy {
  hostelRoomId: number;
  roomId: number;
  roomNumber: string;
  zoneId: number;
  zoneName: string;
  genderRestriction: 'BOYS' | 'GIRLS' | null;
  roomTypeId: number;
  roomTypeName: string;
  sharingCapacity: number;
  occupiedCount: number;
  occupants: RoomAllocationOccupant[];
}
