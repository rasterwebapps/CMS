import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import {
  Page,
  RateContract,
  RateContractRequest,
  RateContractStatusUpdateRequest,
  RateContractStatusUpdateResponse,
} from './rate-contract.model';

@Injectable({ providedIn: 'root' })
export class RateContractService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/procurement/rate-contracts`;

  getPage(p: { supplierId?: number | null; activeOnly?: boolean; page?: number; size?: number }): Observable<Page<RateContract>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.supplierId != null) params = params.set('supplierId', p.supplierId);
    if (p.activeOnly) params = params.set('activeOnly', p.activeOnly);
    return this.http.get<Page<RateContract>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<RateContract> {
    return this.http.get<RateContract>(`${this.baseUrl}/${id}`);
  }

  create(request: RateContractRequest): Observable<RateContract> {
    return this.http.post<RateContract>(this.baseUrl, request);
  }

  update(id: number, request: RateContractRequest): Observable<RateContract> {
    return this.http.put<RateContract>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(id: number, request: RateContractStatusUpdateRequest): Observable<RateContractStatusUpdateResponse> {
    return this.http.patch<RateContractStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }
}
