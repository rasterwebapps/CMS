import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import {
  InventoryLocation,
  InventoryLocationRequest,
  InventoryLocationStatusUpdateRequest,
  InventoryLocationStatusUpdateResponse,
  Page,
} from './inventory-location.model';

@Injectable({ providedIn: 'root' })
export class InventoryLocationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/locations`;

  getAll(activeOnly = false): Observable<InventoryLocation[]> {
    const params = new HttpParams().set('activeOnly', activeOnly);
    return this.http.get<InventoryLocation[]>(this.baseUrl, { params });
  }

  getPage(p: { search?: string; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<InventoryLocation>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<InventoryLocation>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<InventoryLocation> {
    return this.http.get<InventoryLocation>(`${this.baseUrl}/${id}`);
  }

  create(request: InventoryLocationRequest): Observable<InventoryLocation> {
    return this.http.post<InventoryLocation>(this.baseUrl, request);
  }

  update(id: number, request: InventoryLocationRequest): Observable<InventoryLocation> {
    return this.http.put<InventoryLocation>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(id: number, request: InventoryLocationStatusUpdateRequest): Observable<InventoryLocationStatusUpdateResponse> {
    return this.http.patch<InventoryLocationStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  checkNameExists(value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/name-exists`, { params });
  }
}
