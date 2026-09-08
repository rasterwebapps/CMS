import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import { Asset, AssetRequest, AssetStatusUpdateRequest, Page } from './asset.model';

@Injectable({ providedIn: 'root' })
export class AssetService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/asset/assets`;

  getPage(p: { locationId?: number | null; status?: string | null; search?: string | null; page?: number; size?: number }): Observable<Page<Asset>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.locationId != null) params = params.set('locationId', p.locationId);
    if (p.status) params = params.set('status', p.status);
    if (p.search) params = params.set('search', p.search);
    return this.http.get<Page<Asset>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<Asset> {
    return this.http.get<Asset>(`${this.baseUrl}/${id}`);
  }

  create(request: AssetRequest): Observable<Asset> {
    return this.http.post<Asset>(this.baseUrl, request);
  }

  update(id: number, request: AssetRequest): Observable<Asset> {
    return this.http.put<Asset>(`${this.baseUrl}/${id}`, request);
  }

  updateStatus(id: number, request: AssetStatusUpdateRequest): Observable<Asset> {
    return this.http.patch<Asset>(`${this.baseUrl}/${id}/status`, request);
  }

  assetTagExists(value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/asset-tag-exists`, { params });
  }
}
