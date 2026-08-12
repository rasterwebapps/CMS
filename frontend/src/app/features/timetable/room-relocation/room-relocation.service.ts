import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import { RoomRelocationRequestPayload, RoomRelocationResponse, VenueCandidate } from './room-relocation.model';

@Injectable({ providedIn: 'root' })
export class RoomRelocationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/timetables/room-relocation`;

  findCandidates(classScheduleId: number, date: string): Observable<VenueCandidate[]> {
    return this.http.get<VenueCandidate[]>(`${this.baseUrl}/sessions/${classScheduleId}/candidates`, {
      params: { date },
    });
  }

  relocate(classScheduleId: number, request: RoomRelocationRequestPayload): Observable<RoomRelocationResponse> {
    return this.http.post<RoomRelocationResponse>(`${this.baseUrl}/sessions/${classScheduleId}/relocate`, request);
  }

  revert(classScheduleId: number, date: string): Observable<RoomRelocationResponse> {
    return this.http.delete<RoomRelocationResponse>(`${this.baseUrl}/sessions/${classScheduleId}/relocate`, {
      params: { date },
    });
  }
}
