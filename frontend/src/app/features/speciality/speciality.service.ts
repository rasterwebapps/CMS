import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  Speciality,
  SpecialityRequest,
  SpecialityStatusUpdateRequest,
  SpecialityStatusUpdateResponse,
} from './speciality.model';

@Injectable({
  providedIn: 'root',
})
export class SpecialityService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/specialities`;

  getAll(activeOnly = false): Observable<Speciality[]> {
    return this.http.get<Speciality[]>(`${this.baseUrl}?activeOnly=${activeOnly}`);
  }

  getById(id: number): Observable<Speciality> {
    return this.http.get<Speciality>(`${this.baseUrl}/${id}`);
  }

  create(request: SpecialityRequest): Observable<Speciality> {
    return this.http.post<Speciality>(this.baseUrl, request);
  }

  update(id: number, request: SpecialityRequest): Observable<Speciality> {
    return this.http.put<Speciality>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(
    id: number,
    request: SpecialityStatusUpdateRequest,
  ): Observable<SpecialityStatusUpdateResponse> {
    return this.http.patch<SpecialityStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
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
