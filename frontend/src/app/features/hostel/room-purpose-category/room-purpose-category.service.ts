import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import {
  RoomPurposeCategory,
  RoomPurposeCategoryRequest,
  RoomPurposeCategoryStatusUpdateRequest,
  RoomPurposeCategoryStatusUpdateResponse,
  Page,
} from './room-purpose-category.model';

@Injectable({ providedIn: 'root' })
export class RoomPurposeCategoryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/room-purpose-categories`;

  getAll(activeOnly = false): Observable<RoomPurposeCategory[]> {
    return this.http.get<RoomPurposeCategory[]>(`${this.baseUrl}?activeOnly=${activeOnly}`);
  }

  getPage(p: { search?: string; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<RoomPurposeCategory>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<RoomPurposeCategory>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<RoomPurposeCategory> {
    return this.http.get<RoomPurposeCategory>(`${this.baseUrl}/${id}`);
  }

  create(request: RoomPurposeCategoryRequest): Observable<RoomPurposeCategory> {
    return this.http.post<RoomPurposeCategory>(this.baseUrl, request);
  }

  update(id: number, request: RoomPurposeCategoryRequest): Observable<RoomPurposeCategory> {
    return this.http.put<RoomPurposeCategory>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(
    id: number,
    request: RoomPurposeCategoryStatusUpdateRequest,
  ): Observable<RoomPurposeCategoryStatusUpdateResponse> {
    return this.http.patch<RoomPurposeCategoryStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  checkNameExists(value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/name-exists`, { params });
  }

  checkCodeExists(value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/code-exists`, { params });
  }
}
