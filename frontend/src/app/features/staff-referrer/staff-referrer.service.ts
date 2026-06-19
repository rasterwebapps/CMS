import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  StaffReferrer,
  StaffReferrerRequest,
  StaffReferrerStatusUpdateRequest,
  StaffReferrerStatusUpdateResponse,
} from './staff-referrer.model';

@Injectable({ providedIn: 'root' })
export class StaffReferrerService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/staff-referrers`;

  getAll(): Observable<StaffReferrer[]> {
    return this.http.get<StaffReferrer[]>(this.url);
  }

  getActive(): Observable<StaffReferrer[]> {
    return this.http.get<StaffReferrer[]>(`${this.url}?active=true`);
  }

  getById(id: number): Observable<StaffReferrer> {
    return this.http.get<StaffReferrer>(`${this.url}/${id}`);
  }

  create(request: StaffReferrerRequest): Observable<StaffReferrer> {
    return this.http.post<StaffReferrer>(this.url, request);
  }

  update(id: number, request: StaffReferrerRequest): Observable<StaffReferrer> {
    return this.http.put<StaffReferrer>(`${this.url}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  deactivate(id: number): Observable<StaffReferrerStatusUpdateResponse> {
    return this.updateStatus(id, { isActive: false });
  }

  reactivate(id: number): Observable<StaffReferrerStatusUpdateResponse> {
    return this.updateStatus(id, { isActive: true });
  }

  updateStatus(
    id: number,
    request: StaffReferrerStatusUpdateRequest,
  ): Observable<StaffReferrerStatusUpdateResponse> {
    return this.http.patch<StaffReferrerStatusUpdateResponse>(`${this.url}/${id}/status`, request);
  }
}
