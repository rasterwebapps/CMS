import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  BloodGroup,
  BloodGroupRequest,
  BloodGroupStatusUpdateRequest,
  BloodGroupStatusUpdateResponse,
  Page,
} from './blood-group.model';

@Injectable({
  providedIn: 'root',
})
export class BloodGroupService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/blood-groups`;

  getBloodGroups(): Observable<BloodGroup[]> {
    return this.http.get<BloodGroup[]>(this.baseUrl);
  }

  getPage(p: { search?: string; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<BloodGroup>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<BloodGroup>>(`${this.baseUrl}/page`, { params });
  }

  getActiveBloodGroups(): Observable<BloodGroup[]> {
    return this.http.get<BloodGroup[]>(`${this.baseUrl}?activeOnly=true`);
  }

  getBloodGroupById(id: number): Observable<BloodGroup> {
    return this.http.get<BloodGroup>(`${this.baseUrl}/${id}`);
  }

  createBloodGroup(request: BloodGroupRequest): Observable<BloodGroup> {
    return this.http.post<BloodGroup>(this.baseUrl, request);
  }

  updateBloodGroup(id: number, request: BloodGroupRequest): Observable<BloodGroup> {
    return this.http.put<BloodGroup>(`${this.baseUrl}/${id}`, request);
  }

  deleteBloodGroup(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  deactivateBloodGroup(id: number): Observable<BloodGroupStatusUpdateResponse> {
    return this.updateStatus(id, { isActive: false });
  }

  reactivateBloodGroup(id: number): Observable<BloodGroupStatusUpdateResponse> {
    return this.updateStatus(id, { isActive: true });
  }

  updateStatus(
    id: number,
    request: BloodGroupStatusUpdateRequest,
  ): Observable<BloodGroupStatusUpdateResponse> {
    return this.http.patch<BloodGroupStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }
}
