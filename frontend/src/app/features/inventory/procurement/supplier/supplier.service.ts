import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import {
  Page,
  Supplier,
  SupplierRequest,
  SupplierStatusUpdateRequest,
  SupplierStatusUpdateResponse,
} from './supplier.model';

@Injectable({ providedIn: 'root' })
export class SupplierService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/procurement/suppliers`;

  getAll(activeOnly = false): Observable<Supplier[]> {
    const params = new HttpParams().set('activeOnly', activeOnly);
    return this.http.get<Supplier[]>(this.baseUrl, { params });
  }

  getPage(p: { search?: string; page?: number; size?: number }): Observable<Page<Supplier>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    return this.http.get<Page<Supplier>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<Supplier> {
    return this.http.get<Supplier>(`${this.baseUrl}/${id}`);
  }

  create(request: SupplierRequest): Observable<Supplier> {
    return this.http.post<Supplier>(this.baseUrl, request);
  }

  update(id: number, request: SupplierRequest): Observable<Supplier> {
    return this.http.put<Supplier>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(id: number, request: SupplierStatusUpdateRequest): Observable<SupplierStatusUpdateResponse> {
    return this.http.patch<SupplierStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  approve(id: number): Observable<Supplier> {
    return this.http.post<Supplier>(`${this.baseUrl}/${id}/approve`, {});
  }

  checkCodeExists(value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/code-exists`, { params });
  }
}
