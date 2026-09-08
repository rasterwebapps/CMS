import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import {
  Page,
  StockIssueRequest,
  StockIssueRequestAddLineRequest,
  StockIssueRequestCreateRequest,
  StockIssueRequestItem,
  StockIssueRequestResolutionRequest,
} from './stock-issue-request.model';

@Injectable({ providedIn: 'root' })
export class StockIssueRequestService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/issue/stock-issue-requests`;

  getPage(p: { locationId?: number | null; status?: string | null; page?: number; size?: number }): Observable<Page<StockIssueRequest>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.locationId != null) params = params.set('locationId', p.locationId);
    if (p.status) params = params.set('status', p.status);
    return this.http.get<Page<StockIssueRequest>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<StockIssueRequest> {
    return this.http.get<StockIssueRequest>(`${this.baseUrl}/${id}`);
  }

  create(request: StockIssueRequestCreateRequest): Observable<StockIssueRequest> {
    return this.http.post<StockIssueRequest>(this.baseUrl, request);
  }

  addLine(requestId: number, request: StockIssueRequestAddLineRequest): Observable<StockIssueRequestItem> {
    return this.http.post<StockIssueRequestItem>(`${this.baseUrl}/${requestId}/lines`, request);
  }

  removeLine(requestId: number, lineId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${requestId}/lines/${lineId}`);
  }

  submit(requestId: number): Observable<StockIssueRequest> {
    return this.http.post<StockIssueRequest>(`${this.baseUrl}/${requestId}/submit`, {});
  }

  approveLine(requestId: number, lineId: number, request: StockIssueRequestResolutionRequest): Observable<StockIssueRequestItem> {
    return this.http.post<StockIssueRequestItem>(`${this.baseUrl}/${requestId}/lines/${lineId}/approve`, request);
  }

  rejectLine(requestId: number, lineId: number, request: StockIssueRequestResolutionRequest): Observable<StockIssueRequestItem> {
    return this.http.post<StockIssueRequestItem>(`${this.baseUrl}/${requestId}/lines/${lineId}/reject`, request);
  }

  cancel(requestId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${requestId}/cancel`, {});
  }
}
