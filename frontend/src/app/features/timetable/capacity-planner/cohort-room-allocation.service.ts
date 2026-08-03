import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import { CohortRoomAllocation, CohortRoomAllocationCommitRequest } from './cohort-room-allocation.model';

@Injectable({ providedIn: 'root' })
export class CohortRoomAllocationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/timetables/cohort-room-allocations`;

  /** Returns null (204) when no committed allocation exists yet for this cohort/term. */
  getCurrent(cohortId: number, termInstanceId: number): Observable<CohortRoomAllocation | null> {
    const params = { cohortId: cohortId.toString(), termInstanceId: termInstanceId.toString() };
    return this.http.get<CohortRoomAllocation | null>(this.baseUrl, { params });
  }

  commit(request: CohortRoomAllocationCommitRequest): Observable<CohortRoomAllocation> {
    return this.http.post<CohortRoomAllocation>(`${this.baseUrl}/commit`, request);
  }

  revert(allocationId: number): Observable<CohortRoomAllocation> {
    return this.http.post<CohortRoomAllocation>(`${this.baseUrl}/${allocationId}/revert`, {});
  }
}
