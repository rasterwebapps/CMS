export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type ServiceTicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export type ServiceTicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'CANCELLED';

export interface ServiceTicketCreateRequest {
  locationId: number;
  categoryId: number;
  requestedBy: string;
  priority?: ServiceTicketPriority;
  description: string;
}

export interface ServiceTicketAssignRequest {
  assignedTo: string;
}

export interface ServiceTicketResolveRequest {
  resolutionNotes: string;
}

export interface ServiceTicketCloseRequest {
  feedbackRating?: number;
}

export interface ServiceTicketCancelRequest {
  reason: string;
}

export interface ServiceTicket {
  id: number;
  locationId: number;
  locationVirtualName: string;
  categoryId: number;
  categoryName: string;
  requestedBy: string;
  priority: ServiceTicketPriority;
  status: ServiceTicketStatus;
  description: string;
  assignedTo: string | null;
  assignedAt: string | null;
  resolutionNotes: string | null;
  resolutionDate: string | null;
  resolvedBy: string | null;
  feedbackRating: number | null;
  closedBy: string | null;
  closedAt: string | null;
  cancelledBy: string | null;
  cancelledAt: string | null;
  cancellationReason: string | null;
  createdBy: string | null;
  createdAt: string;
  updatedAt: string;
}
