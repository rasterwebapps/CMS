import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import {
  Page,
  ProductLocationReorderConfig,
  ProductLocationReorderConfigRequest,
  ProductLocationReorderConfigStatusUpdateRequest,
  ProductLocationReorderConfigStatusUpdateResponse,
} from './reorder-config.model';

@Injectable({ providedIn: 'root' })
export class ReorderConfigService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/stock/reorder-configs`;

  getPage(p: { productId?: number | null; locationId?: number | null; activeOnly?: boolean; page?: number; size?: number }): Observable<Page<ProductLocationReorderConfig>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.productId != null) params = params.set('productId', p.productId);
    if (p.locationId != null) params = params.set('locationId', p.locationId);
    if (p.activeOnly) params = params.set('activeOnly', p.activeOnly);
    return this.http.get<Page<ProductLocationReorderConfig>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<ProductLocationReorderConfig> {
    return this.http.get<ProductLocationReorderConfig>(`${this.baseUrl}/${id}`);
  }

  create(request: ProductLocationReorderConfigRequest): Observable<ProductLocationReorderConfig> {
    return this.http.post<ProductLocationReorderConfig>(this.baseUrl, request);
  }

  update(id: number, request: ProductLocationReorderConfigRequest): Observable<ProductLocationReorderConfig> {
    return this.http.put<ProductLocationReorderConfig>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(id: number, request: ProductLocationReorderConfigStatusUpdateRequest): Observable<ProductLocationReorderConfigStatusUpdateResponse> {
    return this.http.patch<ProductLocationReorderConfigStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }
}
