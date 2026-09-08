import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import {
  Uom,
  UomRequest,
  UomStatusUpdateRequest,
  UomStatusUpdateResponse,
  Page,
} from './uom.model';

@Injectable({ providedIn: 'root' })
export class UomService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/uoms`;

  getAll(activeOnly = false): Observable<Uom[]> {
    const params = new HttpParams().set('activeOnly', activeOnly);
    return this.http.get<Uom[]>(this.baseUrl, { params });
  }

  getPage(p: { search?: string; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<Uom>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<Uom>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<Uom> {
    return this.http.get<Uom>(`${this.baseUrl}/${id}`);
  }

  create(request: UomRequest): Observable<Uom> {
    return this.http.post<Uom>(this.baseUrl, request);
  }

  update(id: number, request: UomRequest): Observable<Uom> {
    return this.http.put<Uom>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(id: number, request: UomStatusUpdateRequest): Observable<UomStatusUpdateResponse> {
    return this.http.patch<UomStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  checkCodeExists(value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/code-exists`, { params });
  }

  checkNameExists(value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/name-exists`, { params });
  }
}
