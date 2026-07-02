import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  DesignationMaster,
  DesignationRequest,
  DesignationStatusUpdateRequest,
  DesignationStatusUpdateResponse,
  Page,
} from './designation.model';

@Injectable({ providedIn: 'root' })
export class DesignationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/designations`;

  getAll(activeOnly = false): Observable<DesignationMaster[]> {
    return this.http.get<DesignationMaster[]>(`${this.baseUrl}?activeOnly=${activeOnly}`);
  }

  getPage(p: { search?: string; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<DesignationMaster>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<DesignationMaster>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<DesignationMaster> {
    return this.http.get<DesignationMaster>(`${this.baseUrl}/${id}`);
  }

  create(request: DesignationRequest): Observable<DesignationMaster> {
    return this.http.post<DesignationMaster>(this.baseUrl, request);
  }

  update(id: number, request: DesignationRequest): Observable<DesignationMaster> {
    return this.http.put<DesignationMaster>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(
    id: number,
    request: DesignationStatusUpdateRequest,
  ): Observable<DesignationStatusUpdateResponse> {
    return this.http.patch<DesignationStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
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
