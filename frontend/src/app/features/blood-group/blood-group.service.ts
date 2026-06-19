import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  BloodGroup,
  BloodGroupRequest,
  BloodGroupStatusUpdateRequest,
  BloodGroupStatusUpdateResponse,
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
