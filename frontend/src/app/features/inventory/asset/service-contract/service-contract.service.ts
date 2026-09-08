import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import { AssetServiceContract, AssetServiceContractRequest, Page } from './service-contract.model';

@Injectable({ providedIn: 'root' })
export class ServiceContractService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/asset/service-contracts`;

  getPage(p: { assetId?: number | null; activeOnly?: boolean | null; page?: number; size?: number }): Observable<Page<AssetServiceContract>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.assetId != null) params = params.set('assetId', p.assetId);
    if (p.activeOnly) params = params.set('activeOnly', true);
    return this.http.get<Page<AssetServiceContract>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<AssetServiceContract> {
    return this.http.get<AssetServiceContract>(`${this.baseUrl}/${id}`);
  }

  create(request: AssetServiceContractRequest): Observable<AssetServiceContract> {
    return this.http.post<AssetServiceContract>(this.baseUrl, request);
  }

  update(id: number, request: AssetServiceContractRequest): Observable<AssetServiceContract> {
    return this.http.put<AssetServiceContract>(`${this.baseUrl}/${id}`, request);
  }
}
