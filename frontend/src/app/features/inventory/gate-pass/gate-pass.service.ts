import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import {
  GatePass,
  GatePassCreateRequest,
  GatePassRejectRequest,
  GatePassReturnRequest,
  Page,
} from './gate-pass.model';

@Injectable({ providedIn: 'root' })
export class GatePassService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/gate-passes`;

  getPage(p: { locationId?: number | null; direction?: string | null; status?: string | null; overdueOnly?: boolean | null; page?: number; size?: number }): Observable<Page<GatePass>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.locationId != null) params = params.set('locationId', p.locationId);
    if (p.direction) params = params.set('direction', p.direction);
    if (p.status) params = params.set('status', p.status);
    if (p.overdueOnly) params = params.set('overdueOnly', true);
    return this.http.get<Page<GatePass>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<GatePass> {
    return this.http.get<GatePass>(`${this.baseUrl}/${id}`);
  }

  create(request: GatePassCreateRequest): Observable<GatePass> {
    return this.http.post<GatePass>(this.baseUrl, request);
  }

  approve(id: number): Observable<GatePass> {
    return this.http.post<GatePass>(`${this.baseUrl}/${id}/approve`, {});
  }

  reject(id: number, request: GatePassRejectRequest): Observable<GatePass> {
    return this.http.post<GatePass>(`${this.baseUrl}/${id}/reject`, request);
  }

  verifyGate(id: number): Observable<GatePass> {
    return this.http.post<GatePass>(`${this.baseUrl}/${id}/verify-gate`, {});
  }

  markReturned(id: number, request: GatePassReturnRequest): Observable<GatePass> {
    return this.http.post<GatePass>(`${this.baseUrl}/${id}/return`, request);
  }
}
