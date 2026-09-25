import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import { VenueCandidate } from '../room-relocation/room-relocation.model';
import { ApplyRescheduleRequestPayload, RescheduleResponse } from './reschedule.model';

@Injectable({ providedIn: 'root' })
export class RescheduleService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/timetables/reschedule`;

  findCandidates(classScheduleId: number, date: string, targetDate: string, periodId: number): Observable<VenueCandidate[]> {
    return this.http.get<VenueCandidate[]>(`${this.baseUrl}/sessions/${classScheduleId}/candidates`, {
      params: { date, targetDate, periodId },
    });
  }

  apply(classScheduleId: number, request: ApplyRescheduleRequestPayload): Observable<RescheduleResponse> {
    return this.http.post<RescheduleResponse>(`${this.baseUrl}/sessions/${classScheduleId}/apply`, request);
  }

  revert(classScheduleId: number, date: string, targetDate: string): Observable<RescheduleResponse> {
    return this.http.delete<RescheduleResponse>(`${this.baseUrl}/sessions/${classScheduleId}/reschedule`, {
      params: { date, targetDate },
    });
  }
}
