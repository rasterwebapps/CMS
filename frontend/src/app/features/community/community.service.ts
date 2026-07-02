import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  Community,
  CommunityRequest,
  CommunityStatusUpdateRequest,
  CommunityStatusUpdateResponse,
  Page,
} from './community.model';

@Injectable({
  providedIn: 'root',
})
export class CommunityService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/communities`;

  getCommunities(): Observable<Community[]> {
    return this.http.get<Community[]>(this.baseUrl);
  }

  getPage(p: { search?: string; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<Community>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<Community>>(`${this.baseUrl}/page`, { params });
  }

  getActiveCommunities(): Observable<Community[]> {
    return this.http.get<Community[]>(`${this.baseUrl}?activeOnly=true`);
  }

  getCommunityById(id: number): Observable<Community> {
    return this.http.get<Community>(`${this.baseUrl}/${id}`);
  }

  createCommunity(request: CommunityRequest): Observable<Community> {
    return this.http.post<Community>(this.baseUrl, request);
  }

  updateCommunity(id: number, request: CommunityRequest): Observable<Community> {
    return this.http.put<Community>(`${this.baseUrl}/${id}`, request);
  }

  deleteCommunity(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  deactivateCommunity(id: number): Observable<CommunityStatusUpdateResponse> {
    return this.updateStatus(id, { isActive: false });
  }

  reactivateCommunity(id: number): Observable<CommunityStatusUpdateResponse> {
    return this.updateStatus(id, { isActive: true });
  }

  updateStatus(
    id: number,
    request: CommunityStatusUpdateRequest,
  ): Observable<CommunityStatusUpdateResponse> {
    return this.http.patch<CommunityStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }
}
