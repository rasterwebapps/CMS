import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  ClinicalVenue,
  ClinicalVenueRequest,
  ClinicalVenueStatusUpdateRequest,
  ClinicalVenueStatusUpdateResponse,
  Page,
} from './clinical-venue.model';

@Injectable({ providedIn: 'root' })
export class ClinicalVenueService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/clinical-venues`;

  getAll(activeOnly = false): Observable<ClinicalVenue[]> {
    return this.http.get<ClinicalVenue[]>(`${this.baseUrl}?activeOnly=${activeOnly}`);
  }

  getPage(p: { search?: string; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<ClinicalVenue>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<ClinicalVenue>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<ClinicalVenue> {
    return this.http.get<ClinicalVenue>(`${this.baseUrl}/${id}`);
  }

  create(request: ClinicalVenueRequest): Observable<ClinicalVenue> {
    return this.http.post<ClinicalVenue>(this.baseUrl, request);
  }

  update(id: number, request: ClinicalVenueRequest): Observable<ClinicalVenue> {
    return this.http.put<ClinicalVenue>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(
    id: number,
    request: ClinicalVenueStatusUpdateRequest,
  ): Observable<ClinicalVenueStatusUpdateResponse> {
    return this.http.patch<ClinicalVenueStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  checkNameExists(value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/name-exists`, { params });
  }
}
