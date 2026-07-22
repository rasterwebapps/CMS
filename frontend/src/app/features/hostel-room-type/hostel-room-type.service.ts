import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  HostelRoomType,
  HostelRoomTypeRequest,
  HostelRoomTypeStatusUpdateRequest,
  HostelRoomTypeStatusUpdateResponse,
  Page,
} from './hostel-room-type.model';

@Injectable({ providedIn: 'root' })
export class HostelRoomTypeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/hostel-room-types`;

  getAll(activeOnly = false): Observable<HostelRoomType[]> {
    return this.http.get<HostelRoomType[]>(`${this.baseUrl}?activeOnly=${activeOnly}`);
  }

  getPage(p: { search?: string; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<HostelRoomType>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<HostelRoomType>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<HostelRoomType> {
    return this.http.get<HostelRoomType>(`${this.baseUrl}/${id}`);
  }

  create(request: HostelRoomTypeRequest): Observable<HostelRoomType> {
    return this.http.post<HostelRoomType>(this.baseUrl, request);
  }

  update(id: number, request: HostelRoomTypeRequest): Observable<HostelRoomType> {
    return this.http.put<HostelRoomType>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(
    id: number,
    request: HostelRoomTypeStatusUpdateRequest,
  ): Observable<HostelRoomTypeStatusUpdateResponse> {
    return this.http.patch<HostelRoomTypeStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
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
