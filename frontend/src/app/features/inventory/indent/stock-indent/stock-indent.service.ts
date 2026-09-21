import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import {
  Page,
  StockIndent,
  StockIndentAddLineRequest,
  StockIndentCreateRequest,
  StockIndentFulfillViaTransferRequest,
  StockIndentFulfillmentContext,
  StockIndentItem,
  StockIndentRaisePoRequest,
  StockIndentResolutionRequest,
  StockIndentReturnLineRequest,
} from './stock-indent.model';

@Injectable({ providedIn: 'root' })
export class StockIndentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/indent/stock-indents`;

  getPage(p: { locationId?: number | null; status?: string | null; page?: number; size?: number }): Observable<Page<StockIndent>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.locationId != null) params = params.set('locationId', p.locationId);
    if (p.status) params = params.set('status', p.status);
    return this.http.get<Page<StockIndent>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<StockIndent> {
    return this.http.get<StockIndent>(`${this.baseUrl}/${id}`);
  }

  create(request: StockIndentCreateRequest): Observable<StockIndent> {
    return this.http.post<StockIndent>(this.baseUrl, request);
  }

  addLine(requestId: number, request: StockIndentAddLineRequest): Observable<StockIndentItem> {
    return this.http.post<StockIndentItem>(`${this.baseUrl}/${requestId}/lines`, request);
  }

  removeLine(requestId: number, lineId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${requestId}/lines/${lineId}`);
  }

  submit(requestId: number): Observable<StockIndent> {
    return this.http.post<StockIndent>(`${this.baseUrl}/${requestId}/submit`, {});
  }

  approveLine(requestId: number, lineId: number, request: StockIndentResolutionRequest): Observable<StockIndentItem> {
    return this.http.post<StockIndentItem>(`${this.baseUrl}/${requestId}/lines/${lineId}/approve`, request);
  }

  rejectLine(requestId: number, lineId: number, request: StockIndentResolutionRequest): Observable<StockIndentItem> {
    return this.http.post<StockIndentItem>(`${this.baseUrl}/${requestId}/lines/${lineId}/reject`, request);
  }

  returnLine(requestId: number, lineId: number, request: StockIndentReturnLineRequest): Observable<StockIndentItem> {
    return this.http.post<StockIndentItem>(`${this.baseUrl}/${requestId}/lines/${lineId}/return`, request);
  }

  getFulfillmentContext(requestId: number, lineId: number): Observable<StockIndentFulfillmentContext> {
    return this.http.get<StockIndentFulfillmentContext>(`${this.baseUrl}/${requestId}/lines/${lineId}/fulfillment-context`);
  }

  fulfillLine(requestId: number, lineId: number, request: StockIndentResolutionRequest): Observable<StockIndentItem> {
    return this.http.post<StockIndentItem>(`${this.baseUrl}/${requestId}/lines/${lineId}/fulfill`, request);
  }

  fulfillViaTransferLine(requestId: number, lineId: number, request: StockIndentFulfillViaTransferRequest): Observable<StockIndentItem> {
    return this.http.post<StockIndentItem>(`${this.baseUrl}/${requestId}/lines/${lineId}/fulfill-via-transfer`, request);
  }

  raisePoLine(requestId: number, lineId: number, request: StockIndentRaisePoRequest): Observable<StockIndentItem> {
    return this.http.post<StockIndentItem>(`${this.baseUrl}/${requestId}/lines/${lineId}/raise-po`, request);
  }

  denyLine(requestId: number, lineId: number, request: StockIndentResolutionRequest): Observable<StockIndentItem> {
    return this.http.post<StockIndentItem>(`${this.baseUrl}/${requestId}/lines/${lineId}/deny`, request);
  }

  cancel(requestId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${requestId}/cancel`, {});
  }

  runAutoIndent(): Observable<{ created: number }> {
    return this.http.post<{ created: number }>(`${this.baseUrl}/auto-run`, {});
  }
}
