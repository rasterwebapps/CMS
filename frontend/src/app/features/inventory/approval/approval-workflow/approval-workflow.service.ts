import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import { ApprovalWorkflow, ApprovalWorkflowRequest, Page } from './approval-workflow.model';

@Injectable({ providedIn: 'root' })
export class ApprovalWorkflowService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/approval/workflows`;

  getPage(p: { documentType?: string | null; activeOnly?: boolean | null; page?: number; size?: number }): Observable<Page<ApprovalWorkflow>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.documentType) params = params.set('documentType', p.documentType);
    if (p.activeOnly) params = params.set('activeOnly', true);
    return this.http.get<Page<ApprovalWorkflow>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<ApprovalWorkflow> {
    return this.http.get<ApprovalWorkflow>(`${this.baseUrl}/${id}`);
  }

  create(request: ApprovalWorkflowRequest): Observable<ApprovalWorkflow> {
    return this.http.post<ApprovalWorkflow>(this.baseUrl, request);
  }

  update(id: number, request: ApprovalWorkflowRequest): Observable<ApprovalWorkflow> {
    return this.http.put<ApprovalWorkflow>(`${this.baseUrl}/${id}`, request);
  }

  getEligible(documentType: string, locationId: number, amount?: number | null): Observable<ApprovalWorkflow[]> {
    let params = new HttpParams().set('documentType', documentType).set('locationId', locationId);
    if (amount != null) params = params.set('amount', amount);
    return this.http.get<ApprovalWorkflow[]>(`${this.baseUrl}/eligible`, { params });
  }
}
