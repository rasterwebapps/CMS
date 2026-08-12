export interface VenueCandidate {
  id: number;
  name: string;
  capacity: number | null;
}

export interface RoomRelocationRequestPayload {
  date: string;
  venueId: number;
}

export interface RoomRelocationResponse {
  classScheduleId: number;
  date: string;
  venueName: string | null;
  occurrenceStatus: string;
}
