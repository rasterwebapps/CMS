import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  Community,
  CommunityRequest,
  CommunityStatusUpdateRequest,
  CommunityStatusUpdateResponse,
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
