import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import {
  CycleCount,
  CycleCountAddLineRequest,
  CycleCountCreateRequest,
  CycleCountEnterCountRequest,
  CycleCountLine,
  CycleCountResolutionRequest,
  Page,
} from './cycle-count.model';

@Injectable({ providedIn: 'root' })
export class CycleCountService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/stock/cycle-counts`;

  getPage(p: { locationId?: number | null; status?: string | null; page?: number; size?: number }): Observable<Page<CycleCount>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.locationId != null) params = params.set('locationId', p.locationId);
    if (p.status) params = params.set('status', p.status);
    return this.http.get<Page<CycleCount>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<CycleCount> {
    return this.http.get<CycleCount>(`${this.baseUrl}/${id}`);
  }

  create(request: CycleCountCreateRequest): Observable<CycleCount> {
    return this.http.post<CycleCount>(this.baseUrl, request);
  }

  addLine(countId: number, request: CycleCountAddLineRequest): Observable<CycleCountLine> {
    return this.http.post<CycleCountLine>(`${this.baseUrl}/${countId}/lines`, request);
  }

  removeLine(countId: number, lineId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${countId}/lines/${lineId}`);
  }

  enterCount(countId: number, lineId: number, request: CycleCountEnterCountRequest): Observable<CycleCountLine> {
    return this.http.put<CycleCountLine>(`${this.baseUrl}/${countId}/lines/${lineId}/count`, request);
  }

  submit(countId: number): Observable<CycleCount> {
    return this.http.post<CycleCount>(`${this.baseUrl}/${countId}/submit`, {});
  }

  approveLine(countId: number, lineId: number, request: CycleCountResolutionRequest): Observable<CycleCountLine> {
    return this.http.post<CycleCountLine>(`${this.baseUrl}/${countId}/lines/${lineId}/approve`, request);
  }

  rejectLine(countId: number, lineId: number, request: CycleCountResolutionRequest): Observable<CycleCountLine> {
    return this.http.post<CycleCountLine>(`${this.baseUrl}/${countId}/lines/${lineId}/reject`, request);
  }

  cancel(countId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${countId}/cancel`, {});
  }
}
