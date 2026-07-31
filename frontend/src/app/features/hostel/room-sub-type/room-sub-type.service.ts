import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import {
  RoomSubType,
  RoomSubTypeRequest,
  RoomSubTypeStatusUpdateRequest,
  RoomSubTypeStatusUpdateResponse,
  Page,
} from './room-sub-type.model';

@Injectable({ providedIn: 'root' })
export class RoomSubTypeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/room-sub-types`;

  getAll(purposeCategoryId?: number, activeOnly = false): Observable<RoomSubType[]> {
    let params = new HttpParams().set('activeOnly', activeOnly);
    if (purposeCategoryId != null) params = params.set('purposeCategoryId', purposeCategoryId);
    return this.http.get<RoomSubType[]>(this.baseUrl, { params });
  }

  getPage(p: { search?: string; purposeCategoryId?: number; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<RoomSubType>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.purposeCategoryId != null) params = params.set('purposeCategoryId', p.purposeCategoryId);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<RoomSubType>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<RoomSubType> {
    return this.http.get<RoomSubType>(`${this.baseUrl}/${id}`);
  }

  create(request: RoomSubTypeRequest): Observable<RoomSubType> {
    return this.http.post<RoomSubType>(this.baseUrl, request);
  }

  update(id: number, request: RoomSubTypeRequest): Observable<RoomSubType> {
    return this.http.put<RoomSubType>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(
    id: number,
    request: RoomSubTypeStatusUpdateRequest,
  ): Observable<RoomSubTypeStatusUpdateResponse> {
    return this.http.patch<RoomSubTypeStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  checkNameExists(value: string, purposeCategoryId: number, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value).set('purposeCategoryId', purposeCategoryId);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/name-exists`, { params });
  }

  checkCodeExists(value: string, purposeCategoryId: number, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value).set('purposeCategoryId', purposeCategoryId);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/code-exists`, { params });
  }
}
