import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import {
  Brand,
  BrandRequest,
  BrandStatusUpdateRequest,
  BrandStatusUpdateResponse,
  Page,
} from './brand.model';

@Injectable({ providedIn: 'root' })
export class BrandService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/brands`;

  getAll(activeOnly = false): Observable<Brand[]> {
    const params = new HttpParams().set('activeOnly', activeOnly);
    return this.http.get<Brand[]>(this.baseUrl, { params });
  }

  getPage(p: { search?: string; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<Brand>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<Brand>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<Brand> {
    return this.http.get<Brand>(`${this.baseUrl}/${id}`);
  }

  create(request: BrandRequest): Observable<Brand> {
    return this.http.post<Brand>(this.baseUrl, request);
  }

  update(id: number, request: BrandRequest): Observable<Brand> {
    return this.http.put<Brand>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(id: number, request: BrandStatusUpdateRequest): Observable<BrandStatusUpdateResponse> {
    return this.http.patch<BrandStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  checkNameExists(value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/name-exists`, { params });
  }
}
