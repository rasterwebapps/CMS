import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import {
  Category,
  CategoryRequest,
  CategoryStatusUpdateRequest,
  CategoryStatusUpdateResponse,
  Page,
} from './category.model';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/categories`;

  getAll(activeOnly = false): Observable<Category[]> {
    const params = new HttpParams().set('activeOnly', activeOnly);
    return this.http.get<Category[]>(this.baseUrl, { params });
  }

  getPage(p: { search?: string; parentCategoryId?: number | null; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<Category>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.parentCategoryId != null) params = params.set('parentCategoryId', p.parentCategoryId);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<Category>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<Category> {
    return this.http.get<Category>(`${this.baseUrl}/${id}`);
  }

  create(request: CategoryRequest): Observable<Category> {
    return this.http.post<Category>(this.baseUrl, request);
  }

  update(id: number, request: CategoryRequest): Observable<Category> {
    return this.http.put<Category>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(id: number, request: CategoryStatusUpdateRequest): Observable<CategoryStatusUpdateResponse> {
    return this.http.patch<CategoryStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  checkNameExists(value: string, parentCategoryId: number | null, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (parentCategoryId != null) params = params.set('parentCategoryId', parentCategoryId);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/name-exists`, { params });
  }
}
