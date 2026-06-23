import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CommissionRecord, CommissionPayoutRequest } from './commission-explorer.model';

@Injectable({ providedIn: 'root' })
export class CommissionExplorerService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/commission-explorer`;

  getAll(filters: {
    status?: string;
    source?: string;
    referralTypeId?: number;
    agentId?: number;
    fromDate?: string;
    toDate?: string;
    search?: string;
  } = {}): Observable<CommissionRecord[]> {
    let params = new HttpParams();
    if (filters.status)        params = params.set('status', filters.status);
    if (filters.source)        params = params.set('source', filters.source);
    if (filters.referralTypeId) params = params.set('referralTypeId', filters.referralTypeId);
    if (filters.agentId)       params = params.set('agentId', filters.agentId);
    if (filters.fromDate)      params = params.set('fromDate', filters.fromDate);
    if (filters.toDate)        params = params.set('toDate', filters.toDate);
    if (filters.search)        params = params.set('search', filters.search);
    return this.http.get<CommissionRecord[]>(this.base, { params });
  }

  recordPayout(enquiryId: number, request: CommissionPayoutRequest): Observable<CommissionRecord> {
    return this.http.post<CommissionRecord>(`${this.base}/${enquiryId}/payouts`, request);
  }

  approve(enquiryId: number): Observable<CommissionRecord> {
    return this.http.post<CommissionRecord>(`${this.base}/${enquiryId}/approve`, {});
  }

  reject(enquiryId: number, reason: string): Observable<CommissionRecord> {
    return this.http.post<CommissionRecord>(`${this.base}/${enquiryId}/reject`, { reason });
  }

  reopen(enquiryId: number): Observable<CommissionRecord> {
    return this.http.post<CommissionRecord>(`${this.base}/${enquiryId}/reopen`, {});
  }
}
