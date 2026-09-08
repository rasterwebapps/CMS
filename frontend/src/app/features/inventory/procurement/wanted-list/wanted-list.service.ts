import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import { PurchaseRequisition } from '../purchase-requisition/purchase-requisition.model';
import {
  Page,
  WantedListConvertRequest,
  WantedListItem,
  WantedListRejectRequest,
  WantedListResolutionRequest,
} from './wanted-list.model';

@Injectable({ providedIn: 'root' })
export class WantedListService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/procurement/wanted-list`;

  getPage(p: { locationId?: number | null; status?: string | null; page?: number; size?: number }): Observable<Page<WantedListItem>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.locationId != null) params = params.set('locationId', p.locationId);
    if (p.status) params = params.set('status', p.status);
    return this.http.get<Page<WantedListItem>>(`${this.baseUrl}/page`, { params });
  }

  run(): Observable<{ created: number }> {
    return this.http.post<{ created: number }>(`${this.baseUrl}/run`, {});
  }

  defer(id: number, request: WantedListResolutionRequest): Observable<WantedListItem> {
    return this.http.post<WantedListItem>(`${this.baseUrl}/${id}/defer`, request);
  }

  reopen(id: number): Observable<WantedListItem> {
    return this.http.post<WantedListItem>(`${this.baseUrl}/${id}/reopen`, {});
  }

  reject(id: number, request: WantedListRejectRequest): Observable<WantedListItem> {
    return this.http.post<WantedListItem>(`${this.baseUrl}/${id}/reject`, request);
  }

  convert(request: WantedListConvertRequest): Observable<PurchaseRequisition> {
    return this.http.post<PurchaseRequisition>(`${this.baseUrl}/convert`, request);
  }
}
