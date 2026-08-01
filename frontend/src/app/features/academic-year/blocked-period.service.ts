import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { BlockedPeriod, BlockedPeriodRequest } from './academic-year.model';

@Injectable({ providedIn: 'root' })
export class BlockedPeriodService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/blocked-periods`;

  getAll(): Observable<BlockedPeriod[]> {
    return this.http.get<BlockedPeriod[]>(this.baseUrl);
  }

  create(request: BlockedPeriodRequest): Observable<BlockedPeriod> {
    return this.http.post<BlockedPeriod>(this.baseUrl, request);
  }

  update(id: number, request: BlockedPeriodRequest): Observable<BlockedPeriod> {
    return this.http.put<BlockedPeriod>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
