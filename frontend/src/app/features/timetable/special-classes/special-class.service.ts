import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import {
  DayRepeatRequestPayload,
  DayRepeatResult,
  SpecialClassOccurrence,
  SpecialClassRequestPayload,
} from './special-class.model';

@Injectable({ providedIn: 'root' })
export class SpecialClassService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/timetables/special-classes`;

  requestSingleSubject(request: SpecialClassRequestPayload): Observable<SpecialClassOccurrence> {
    return this.http.post<SpecialClassOccurrence>(`${this.baseUrl}/single-subject`, request);
  }

  requestDayRepeat(request: DayRepeatRequestPayload): Observable<DayRepeatResult> {
    return this.http.post<DayRepeatResult>(`${this.baseUrl}/day-repeat`, request);
  }

  myRequests(): Observable<SpecialClassOccurrence[]> {
    return this.http.get<SpecialClassOccurrence[]>(`${this.baseUrl}/my-requests`);
  }

  approvalQueue(): Observable<SpecialClassOccurrence[]> {
    return this.http.get<SpecialClassOccurrence[]>(`${this.baseUrl}/approval-queue`);
  }

  approve(id: number): Observable<SpecialClassOccurrence> {
    return this.http.put<SpecialClassOccurrence>(`${this.baseUrl}/${id}/approve`, {});
  }

  approveBatch(requestBatchId: string): Observable<SpecialClassOccurrence[]> {
    return this.http.put<SpecialClassOccurrence[]>(`${this.baseUrl}/batches/${requestBatchId}/approve`, {});
  }

  reject(id: number, rejectionReason: string): Observable<SpecialClassOccurrence> {
    return this.http.put<SpecialClassOccurrence>(`${this.baseUrl}/${id}/reject`, { rejectionReason });
  }

  rejectBatch(requestBatchId: string, rejectionReason: string): Observable<SpecialClassOccurrence[]> {
    return this.http.put<SpecialClassOccurrence[]>(`${this.baseUrl}/batches/${requestBatchId}/reject`, { rejectionReason });
  }

  cancel(id: number): Observable<SpecialClassOccurrence> {
    return this.http.put<SpecialClassOccurrence>(`${this.baseUrl}/${id}/cancel`, {});
  }
}
