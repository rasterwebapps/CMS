import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { EscortCandidate, EscortDuty, EscortRotationPool, EscortRotationPoolRequest } from './escort-rotation.model';

@Injectable({ providedIn: 'root' })
export class EscortRotationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/escort-rotation`;

  eligibleCandidates(batchId: number): Observable<EscortCandidate[]> {
    return this.http.get<EscortCandidate[]>(`${this.baseUrl}/batches/${batchId}/candidates`);
  }

  setupPool(request: EscortRotationPoolRequest): Observable<EscortRotationPool> {
    return this.http.post<EscortRotationPool>(`${this.baseUrl}/pools`, request);
  }

  deactivatePool(batchId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/batches/${batchId}/pool`);
  }

  getPool(batchId: number): Observable<EscortRotationPool | null> {
    return this.http.get<EscortRotationPool | null>(`${this.baseUrl}/batches/${batchId}/pool`);
  }

  resolveForDate(batchId: number, date: string): Observable<EscortDuty | null> {
    return this.http.get<EscortDuty | null>(`${this.baseUrl}/batches/${batchId}/resolve`, { params: { date } });
  }

  myUpcomingDuties(): Observable<EscortDuty[]> {
    return this.http.get<EscortDuty[]>(`${this.baseUrl}/my-duties`);
  }
}
