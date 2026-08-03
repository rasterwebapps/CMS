export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ClinicalVenue {
  id: number;
  name: string;
  hospitalName?: string;
  department?: string;
  capacity?: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  /** Linked physical Campus Setup Room — only set for an on-campus clinical/skills space; an
   *  off-campus hospital posting has no Room to link and stays described by hospitalName/department. */
  roomId?: number;
  roomLabel?: string;
}

export interface ClinicalVenueRequest {
  name: string;
  hospitalName?: string;
  department?: string;
  capacity?: number;
  isActive?: boolean;
  roomId?: number;
}

export interface ClinicalVenueStatusUpdateRequest {
  isActive: boolean;
  reason?: string;
}

export interface ClinicalVenueStatusUpdateResponse {
  id: number;
  isActive: boolean;
  updatedAt: string;
}
