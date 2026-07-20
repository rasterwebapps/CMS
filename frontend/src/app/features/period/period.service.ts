import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  Period,
  PeriodRequest,
  PeriodStatusUpdateRequest,
  PeriodStatusUpdateResponse,
  Page,
} from './period.model';

@Injectable({ providedIn: 'root' })
export class PeriodService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/periods`;

  getAll(activeOnly = false): Observable<Period[]> {
    return this.http.get<Period[]>(`${this.baseUrl}?activeOnly=${activeOnly}`);
  }

  getPage(p: { search?: string; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<Period>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<Period>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<Period> {
    return this.http.get<Period>(`${this.baseUrl}/${id}`);
  }

  create(request: PeriodRequest): Observable<Period> {
    return this.http.post<Period>(this.baseUrl, request);
  }

  update(id: number, request: PeriodRequest): Observable<Period> {
    return this.http.put<Period>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(
    id: number,
    request: PeriodStatusUpdateRequest,
  ): Observable<PeriodStatusUpdateResponse> {
    return this.http.patch<PeriodStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  checkNameExists(value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/name-exists`, { params });
  }
}
