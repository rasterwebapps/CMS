import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  Institution,
  InstitutionRequest,
  InstitutionStatusUpdateRequest,
  InstitutionStatusUpdateResponse,
} from './institution.model';

@Injectable({ providedIn: 'root' })
export class InstitutionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/institutions`;

  getAll(activeOnly = false): Observable<Institution[]> {
    return this.http.get<Institution[]>(`${this.baseUrl}?activeOnly=${activeOnly}`);
  }

  getById(id: number): Observable<Institution> {
    return this.http.get<Institution>(`${this.baseUrl}/${id}`);
  }

  create(request: InstitutionRequest): Observable<Institution> {
    return this.http.post<Institution>(this.baseUrl, request);
  }

  update(id: number, request: InstitutionRequest): Observable<Institution> {
    return this.http.put<Institution>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(
    id: number,
    request: InstitutionStatusUpdateRequest,
  ): Observable<InstitutionStatusUpdateResponse> {
    return this.http.patch<InstitutionStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  checkNameExists(value: string, excludeId?: number): Observable<boolean> {
    let url = `${this.baseUrl}/name-exists?value=${encodeURIComponent(value)}`;
    if (excludeId != null) url += `&excludeId=${excludeId}`;
    return this.http.get<boolean>(url);
  }

  checkCodeExists(value: string, excludeId?: number): Observable<boolean> {
    let url = `${this.baseUrl}/code-exists?value=${encodeURIComponent(value)}`;
    if (excludeId != null) url += `&excludeId=${excludeId}`;
    return this.http.get<boolean>(url);
  }
}
