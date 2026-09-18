import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import { ConflictAcknowledgmentStatus, ConflictScanResponse } from './conflict-inspector.model';

@Injectable({ providedIn: 'root' })
export class ConflictInspectorService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/timetables/conflict-inspector`;

  scan(termInstanceId: number): Observable<ConflictScanResponse> {
    return this.http.get<ConflictScanResponse>(this.baseUrl, {
      params: { termInstanceId: termInstanceId.toString() },
    });
  }

  getAcknowledgmentStatus(termInstanceId: number): Observable<ConflictAcknowledgmentStatus> {
    return this.http.get<ConflictAcknowledgmentStatus>(`${this.baseUrl}/acknowledgment-status`, {
      params: { termInstanceId: termInstanceId.toString() },
    });
  }

  /** Rejects (409, same shape as {@link scan} rows' violations) if the term isn't actually clean
   *  at the moment of calling — the backend always re-scans rather than trusting the caller's last
   *  fetched {@link ConflictScanResponse}. */
  acknowledge(termInstanceId: number): Observable<ConflictAcknowledgmentStatus> {
    return this.http.post<ConflictAcknowledgmentStatus>(`${this.baseUrl}/acknowledge`, null, {
      params: { termInstanceId: termInstanceId.toString() },
    });
  }
}
