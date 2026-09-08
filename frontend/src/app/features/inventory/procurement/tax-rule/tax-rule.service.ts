import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import {
  Page,
  TaxRule,
  TaxRuleRequest,
  TaxRuleStatusUpdateRequest,
  TaxRuleStatusUpdateResponse,
} from './tax-rule.model';

@Injectable({ providedIn: 'root' })
export class TaxRuleService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/procurement/tax-rules`;

  getAll(activeOnly = false): Observable<TaxRule[]> {
    const params = new HttpParams().set('activeOnly', activeOnly);
    return this.http.get<TaxRule[]>(this.baseUrl, { params });
  }

  getPage(p: { search?: string; page?: number; size?: number }): Observable<Page<TaxRule>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    return this.http.get<Page<TaxRule>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<TaxRule> {
    return this.http.get<TaxRule>(`${this.baseUrl}/${id}`);
  }

  create(request: TaxRuleRequest): Observable<TaxRule> {
    return this.http.post<TaxRule>(this.baseUrl, request);
  }

  update(id: number, request: TaxRuleRequest): Observable<TaxRule> {
    return this.http.put<TaxRule>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(id: number, request: TaxRuleStatusUpdateRequest): Observable<TaxRuleStatusUpdateResponse> {
    return this.http.patch<TaxRuleStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  checkNameExists(value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/name-exists`, { params });
  }
}
