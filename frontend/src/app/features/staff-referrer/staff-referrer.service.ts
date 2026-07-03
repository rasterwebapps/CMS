import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  StaffReferrer,
  StaffReferrerRequest,
  StaffReferrerStatusUpdateRequest,
  StaffReferrerStatusUpdateResponse,
  Page,
} from './staff-referrer.model';

@Injectable({ providedIn: 'root' })
export class StaffReferrerService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/staff-referrers`;

  getAll(): Observable<StaffReferrer[]> {
    return this.http.get<StaffReferrer[]>(this.url);
  }

  getPage(p: { search?: string; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<StaffReferrer>> {
    let params = new HttpParams()
      .set('page', p.page ?? 0)
      .set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.sort)   params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<StaffReferrer>>(`${this.url}/page`, { params });
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

  exportStaffReferrers(format: 'excel' | 'pdf', filters: { search?: string | null } = {}): Observable<Blob> {
    let params = new HttpParams().set('format', format);
    if (filters.search) params = params.set('search', filters.search);
    return this.http.get(`${this.url}/export`, { params, responseType: 'blob' });
  }
}
