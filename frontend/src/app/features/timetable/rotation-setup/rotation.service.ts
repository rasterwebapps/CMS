import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import { RotationCandidateSlot, RotationGroupCreateRequest, RotationGroupResponse } from './rotation.model';

@Injectable({ providedIn: 'root' })
export class RotationGroupService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/timetables/rotation-groups`;

  list(termInstanceId: number): Observable<RotationGroupResponse[]> {
    return this.http.get<RotationGroupResponse[]>(this.baseUrl, {
      params: { termInstanceId: termInstanceId.toString() },
    });
  }

  candidateSlots(termInstanceId: number, dayOfWeek: string, periodId: number): Observable<RotationCandidateSlot[]> {
    return this.http.get<RotationCandidateSlot[]>(`${this.baseUrl}/candidate-slots`, {
      params: { termInstanceId: termInstanceId.toString(), dayOfWeek, periodId: periodId.toString() },
    });
  }

  create(request: RotationGroupCreateRequest): Observable<RotationGroupResponse> {
    return this.http.post<RotationGroupResponse>(this.baseUrl, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
