export type RoomPreferenceStatus = 'PENDING' | 'FULFILLED' | 'CANCELLED';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface RoomPreference {
  id: number;
  enquiryId: number | null;
  enquiryName: string | null;
  studentId: number | null;
  studentName: string | null;
  preferredRoomTypeId: number;
  preferredRoomTypeName: string;
  preferredZoneId: number | null;
  preferredZoneName: string | null;
  status: RoomPreferenceStatus;
  remarks?: string;
  createdAt: string;
  updatedAt: string;
}

export interface RoomPreferenceRequest {
  enquiryId?: number | null;
  studentId?: number | null;
  preferredRoomTypeId: number;
  preferredZoneId?: number | null;
  status?: RoomPreferenceStatus;
  remarks?: string;
}
