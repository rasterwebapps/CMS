import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import {
  ApprovalActionBypassRequest,
  ApprovalActionResolutionRequest,
  ApprovalInstance,
  ApprovalInstanceStartRequest,
  Page,
} from './approval-instance.model';

@Injectable({ providedIn: 'root' })
export class ApprovalInstanceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/approval/instances`;

  getPage(p: { documentType?: string | null; status?: string | null; page?: number; size?: number }): Observable<Page<ApprovalInstance>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.documentType) params = params.set('documentType', p.documentType);
    if (p.status) params = params.set('status', p.status);
    return this.http.get<Page<ApprovalInstance>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<ApprovalInstance> {
    return this.http.get<ApprovalInstance>(`${this.baseUrl}/${id}`);
  }

  start(request: ApprovalInstanceStartRequest): Observable<ApprovalInstance> {
    return this.http.post<ApprovalInstance>(`${this.baseUrl}/start`, request);
  }

  approveAction(instanceId: number, actionId: number, request: ApprovalActionResolutionRequest): Observable<ApprovalInstance> {
    return this.http.post<ApprovalInstance>(`${this.baseUrl}/${instanceId}/actions/${actionId}/approve`, request);
  }

  rejectAction(instanceId: number, actionId: number, request: ApprovalActionResolutionRequest): Observable<ApprovalInstance> {
    return this.http.post<ApprovalInstance>(`${this.baseUrl}/${instanceId}/actions/${actionId}/reject`, request);
  }

  bypassAction(instanceId: number, actionId: number, request: ApprovalActionBypassRequest): Observable<ApprovalInstance> {
    return this.http.post<ApprovalInstance>(`${this.baseUrl}/${instanceId}/actions/${actionId}/bypass`, request);
  }
}
