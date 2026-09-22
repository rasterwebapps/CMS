import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import { PurchaseOrder } from '../purchase-order/purchase-order.model';
import { DocumentNumberRegenerationResult } from '../../../../shared/models/document-number.model';
import {
  AvailableRequisitionLine,
  Page,
  QuotationRequest,
  QuotationRequestAddLineRequest,
  QuotationRequestAddSupplierRequest,
  QuotationRequestAwardRequest,
  QuotationRequestCreateRequest,
  QuotationRequestLine,
  QuotationRequestSupplier,
  QuotationResponseLine,
  QuotationResponseLineRequest,
} from './quotation-request.model';

@Injectable({ providedIn: 'root' })
export class QuotationRequestService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/procurement/quotation-requests`;

  getPage(p: { locationId?: number | null; status?: string | null; search?: string; page?: number; size?: number }): Observable<Page<QuotationRequest>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.locationId != null) params = params.set('locationId', p.locationId);
    if (p.status) params = params.set('status', p.status);
    if (p.search) params = params.set('search', p.search);
    return this.http.get<Page<QuotationRequest>>(`${this.baseUrl}/page`, { params });
  }

  previewRegenerateNumbers(): Observable<DocumentNumberRegenerationResult> {
    return this.http.get<DocumentNumberRegenerationResult>(`${this.baseUrl}/regenerate-numbers/preview`);
  }

  regenerateNumbers(): Observable<DocumentNumberRegenerationResult> {
    return this.http.post<DocumentNumberRegenerationResult>(`${this.baseUrl}/regenerate-numbers`, {});
  }

  getById(id: number): Observable<QuotationRequest> {
    return this.http.get<QuotationRequest>(`${this.baseUrl}/${id}`);
  }

  getAvailableRequisitionLines(locationId: number): Observable<AvailableRequisitionLine[]> {
    const params = new HttpParams().set('locationId', locationId);
    return this.http.get<AvailableRequisitionLine[]>(`${this.baseUrl}/available-requisition-lines`, { params });
  }

  create(request: QuotationRequestCreateRequest): Observable<QuotationRequest> {
    return this.http.post<QuotationRequest>(this.baseUrl, request);
  }

  addLine(requestId: number, request: QuotationRequestAddLineRequest): Observable<QuotationRequestLine> {
    return this.http.post<QuotationRequestLine>(`${this.baseUrl}/${requestId}/lines`, request);
  }

  removeLine(requestId: number, lineId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${requestId}/lines/${lineId}`);
  }

  addSupplier(requestId: number, request: QuotationRequestAddSupplierRequest): Observable<QuotationRequestSupplier> {
    return this.http.post<QuotationRequestSupplier>(`${this.baseUrl}/${requestId}/suppliers`, request);
  }

  removeSupplier(requestId: number, supplierId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${requestId}/suppliers/${supplierId}`);
  }

  submit(requestId: number): Observable<QuotationRequest> {
    return this.http.post<QuotationRequest>(`${this.baseUrl}/${requestId}/submit`, {});
  }

  recordResponse(requestId: number, lineId: number, supplierId: number, request: QuotationResponseLineRequest): Observable<QuotationResponseLine> {
    return this.http.post<QuotationResponseLine>(`${this.baseUrl}/${requestId}/lines/${lineId}/responses/${supplierId}`, request);
  }

  award(requestId: number, lineId: number, request: QuotationRequestAwardRequest): Observable<QuotationRequestLine> {
    return this.http.post<QuotationRequestLine>(`${this.baseUrl}/${requestId}/lines/${lineId}/award`, request);
  }

  rejectLine(requestId: number, lineId: number): Observable<QuotationRequestLine> {
    return this.http.post<QuotationRequestLine>(`${this.baseUrl}/${requestId}/lines/${lineId}/reject`, {});
  }

  convertAwardedLines(requestId: number): Observable<PurchaseOrder[]> {
    return this.http.post<PurchaseOrder[]>(`${this.baseUrl}/${requestId}/convert`, {});
  }

  cancel(requestId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${requestId}/cancel`, {});
  }
}
