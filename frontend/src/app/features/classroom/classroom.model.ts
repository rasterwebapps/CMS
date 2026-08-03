export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface Classroom {
  id: number;
  name: string;
  building?: string;
  roomNumber?: string;
  capacity?: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  /** Linked physical Campus Setup Room, if any — see CmsRoomPickerComponent. */
  roomId?: number;
  roomLabel?: string;
}

export interface ClassroomRequest {
  name: string;
  building?: string;
  roomNumber?: string;
  capacity?: number;
  isActive?: boolean;
  roomId?: number;
}

export interface ClassroomStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface ClassroomStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
