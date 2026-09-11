import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import {
  CurrencyExchangeRate,
  CurrencyExchangeRateRequest,
  CurrencyExchangeRateStatusUpdateRequest,
  CurrencyExchangeRateStatusUpdateResponse,
  Page,
} from './currency-exchange-rate.model';

@Injectable({ providedIn: 'root' })
export class CurrencyExchangeRateService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/procurement/currency-exchange-rates`;

  getAll(): Observable<CurrencyExchangeRate[]> {
    return this.http.get<CurrencyExchangeRate[]>(this.baseUrl);
  }

  getPage(p: { search?: string; page?: number; size?: number }): Observable<Page<CurrencyExchangeRate>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    return this.http.get<Page<CurrencyExchangeRate>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<CurrencyExchangeRate> {
    return this.http.get<CurrencyExchangeRate>(`${this.baseUrl}/${id}`);
  }

  create(request: CurrencyExchangeRateRequest): Observable<CurrencyExchangeRate> {
    return this.http.post<CurrencyExchangeRate>(this.baseUrl, request);
  }

  update(id: number, request: CurrencyExchangeRateRequest): Observable<CurrencyExchangeRate> {
    return this.http.put<CurrencyExchangeRate>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(id: number, request: CurrencyExchangeRateStatusUpdateRequest): Observable<CurrencyExchangeRateStatusUpdateResponse> {
    return this.http.patch<CurrencyExchangeRateStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  // value = effectiveDate (yyyy-MM-dd), currencyCode = scope — matches the shared
  // uniqueFieldValidator convention (see VendorProductMapping's pair-exists).
  checkPairExists(effectiveDate: string, currencyCode: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', effectiveDate).set('currencyCode', currencyCode);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/pair-exists`, { params });
  }
}
