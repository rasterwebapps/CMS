import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import {
  Page,
  VendorProductMapping,
  VendorProductMappingRequest,
  VendorProductMappingStatusUpdateRequest,
  VendorProductMappingStatusUpdateResponse,
} from './vendor-product-mapping.model';

@Injectable({ providedIn: 'root' })
export class VendorProductMappingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/procurement/vendor-product-mappings`;

  getPage(p: { supplierId?: number | null; productId?: number | null; activeOnly?: boolean; page?: number; size?: number }): Observable<Page<VendorProductMapping>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.supplierId != null) params = params.set('supplierId', p.supplierId);
    if (p.productId != null) params = params.set('productId', p.productId);
    if (p.activeOnly) params = params.set('activeOnly', p.activeOnly);
    return this.http.get<Page<VendorProductMapping>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<VendorProductMapping> {
    return this.http.get<VendorProductMapping>(`${this.baseUrl}/${id}`);
  }

  create(request: VendorProductMappingRequest): Observable<VendorProductMapping> {
    return this.http.post<VendorProductMapping>(this.baseUrl, request);
  }

  update(id: number, request: VendorProductMappingRequest): Observable<VendorProductMapping> {
    return this.http.put<VendorProductMapping>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(id: number, request: VendorProductMappingStatusUpdateRequest): Observable<VendorProductMappingStatusUpdateResponse> {
    return this.http.patch<VendorProductMappingStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }
}
