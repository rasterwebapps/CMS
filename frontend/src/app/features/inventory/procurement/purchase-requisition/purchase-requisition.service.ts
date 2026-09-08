import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import {
  Page,
  PurchaseRequisition,
  PurchaseRequisitionAddLineRequest,
  PurchaseRequisitionCreateRequest,
  PurchaseRequisitionItem,
  PurchaseRequisitionResolutionRequest,
} from './purchase-requisition.model';

@Injectable({ providedIn: 'root' })
export class PurchaseRequisitionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/procurement/purchase-requisitions`;

  getPage(p: { locationId?: number | null; status?: string | null; page?: number; size?: number }): Observable<Page<PurchaseRequisition>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.locationId != null) params = params.set('locationId', p.locationId);
    if (p.status) params = params.set('status', p.status);
    return this.http.get<Page<PurchaseRequisition>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<PurchaseRequisition> {
    return this.http.get<PurchaseRequisition>(`${this.baseUrl}/${id}`);
  }

  create(request: PurchaseRequisitionCreateRequest): Observable<PurchaseRequisition> {
    return this.http.post<PurchaseRequisition>(this.baseUrl, request);
  }

  addLine(requisitionId: number, request: PurchaseRequisitionAddLineRequest): Observable<PurchaseRequisitionItem> {
    return this.http.post<PurchaseRequisitionItem>(`${this.baseUrl}/${requisitionId}/lines`, request);
  }

  removeLine(requisitionId: number, lineId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${requisitionId}/lines/${lineId}`);
  }

  submit(requisitionId: number): Observable<PurchaseRequisition> {
    return this.http.post<PurchaseRequisition>(`${this.baseUrl}/${requisitionId}/submit`, {});
  }

  approveLine(requisitionId: number, lineId: number, request: PurchaseRequisitionResolutionRequest): Observable<PurchaseRequisitionItem> {
    return this.http.post<PurchaseRequisitionItem>(`${this.baseUrl}/${requisitionId}/lines/${lineId}/approve`, request);
  }

  rejectLine(requisitionId: number, lineId: number, request: PurchaseRequisitionResolutionRequest): Observable<PurchaseRequisitionItem> {
    return this.http.post<PurchaseRequisitionItem>(`${this.baseUrl}/${requisitionId}/lines/${lineId}/reject`, request);
  }

  cancel(requisitionId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${requisitionId}/cancel`, {});
  }
}
