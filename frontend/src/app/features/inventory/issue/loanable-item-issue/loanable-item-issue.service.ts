import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import {
  LoanableItemIssue,
  LoanableItemIssueCreateRequest,
  LoanableItemIssueReturnRequest,
  Page,
} from './loanable-item-issue.model';

@Injectable({ providedIn: 'root' })
export class LoanableItemIssueService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/issue/loanable-item-issues`;

  getPage(p: { locationId?: number | null; status?: string | null; overdueOnly?: boolean | null; page?: number; size?: number }): Observable<Page<LoanableItemIssue>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.locationId != null) params = params.set('locationId', p.locationId);
    if (p.status) params = params.set('status', p.status);
    if (p.overdueOnly) params = params.set('overdueOnly', true);
    return this.http.get<Page<LoanableItemIssue>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<LoanableItemIssue> {
    return this.http.get<LoanableItemIssue>(`${this.baseUrl}/${id}`);
  }

  create(request: LoanableItemIssueCreateRequest): Observable<LoanableItemIssue> {
    return this.http.post<LoanableItemIssue>(this.baseUrl, request);
  }

  markReturned(id: number, request: LoanableItemIssueReturnRequest): Observable<LoanableItemIssue> {
    return this.http.post<LoanableItemIssue>(`${this.baseUrl}/${id}/return`, request);
  }
}
