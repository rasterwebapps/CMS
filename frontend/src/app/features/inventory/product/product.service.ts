import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import {
  Product,
  ProductRequest,
  ProductStatusUpdateRequest,
  ProductStatusUpdateResponse,
  Page,
} from './product.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/products`;

  getPage(p: { search?: string; categoryId?: number | null; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<Product>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.categoryId != null) params = params.set('categoryId', p.categoryId);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<Product>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/${id}`);
  }

  create(request: ProductRequest): Observable<Product> {
    return this.http.post<Product>(this.baseUrl, request);
  }

  update(id: number, request: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(id: number, request: ProductStatusUpdateRequest): Observable<ProductStatusUpdateResponse> {
    return this.http.patch<ProductStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  checkCodeExists(value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/code-exists`, { params });
  }

  checkNameExists(value: string, categoryId: number | null, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (categoryId != null) params = params.set('categoryId', categoryId);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/name-exists`, { params });
  }
}
