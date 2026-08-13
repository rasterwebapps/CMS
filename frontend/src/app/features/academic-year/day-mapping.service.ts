import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { DayMapping, DayMappingRequest } from './academic-year.model';

@Injectable({ providedIn: 'root' })
export class DayMappingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/day-mappings`;

  getAll(): Observable<DayMapping[]> {
    return this.http.get<DayMapping[]>(this.baseUrl);
  }

  create(request: DayMappingRequest): Observable<DayMapping> {
    return this.http.post<DayMapping>(this.baseUrl, request);
  }

  update(id: number, request: DayMappingRequest): Observable<DayMapping> {
    return this.http.put<DayMapping>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
