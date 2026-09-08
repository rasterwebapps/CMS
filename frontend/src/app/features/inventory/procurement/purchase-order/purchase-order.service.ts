import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import {
  AvailableRequisitionLine,
  Page,
  PurchaseOrder,
  PurchaseOrderAddLineRequest,
  PurchaseOrderCreateRequest,
  PurchaseOrderForceCloseRequest,
  PurchaseOrderItem,
} from './purchase-order.model';

@Injectable({ providedIn: 'root' })
export class PurchaseOrderService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/procurement/purchase-orders`;

  getPage(p: { supplierId?: number | null; locationId?: number | null; status?: string | null; page?: number; size?: number }): Observable<Page<PurchaseOrder>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.supplierId != null) params = params.set('supplierId', p.supplierId);
    if (p.locationId != null) params = params.set('locationId', p.locationId);
    if (p.status) params = params.set('status', p.status);
    return this.http.get<Page<PurchaseOrder>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<PurchaseOrder> {
    return this.http.get<PurchaseOrder>(`${this.baseUrl}/${id}`);
  }

  create(request: PurchaseOrderCreateRequest): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(this.baseUrl, request);
  }

  getAvailableRequisitionLines(locationId: number): Observable<AvailableRequisitionLine[]> {
    const params = new HttpParams().set('locationId', locationId);
    return this.http.get<AvailableRequisitionLine[]>(`${this.baseUrl}/available-requisition-lines`, { params });
  }

  addLine(orderId: number, request: PurchaseOrderAddLineRequest): Observable<PurchaseOrderItem> {
    return this.http.post<PurchaseOrderItem>(`${this.baseUrl}/${orderId}/lines`, request);
  }

  removeLine(orderId: number, lineId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${orderId}/lines/${lineId}`);
  }

  order(orderId: number): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${this.baseUrl}/${orderId}/order`, {});
  }

  forceClose(orderId: number, request: PurchaseOrderForceCloseRequest): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${this.baseUrl}/${orderId}/force-close`, request);
  }
}
