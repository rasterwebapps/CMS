import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import { StockValuationReport } from './stock-valuation.model';

@Injectable({ providedIn: 'root' })
export class StockValuationReportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/reporting/stock-valuation`;

  get(locationId?: number | null): Observable<StockValuationReport> {
    let params = new HttpParams();
    if (locationId != null) params = params.set('locationId', locationId);
    return this.http.get<StockValuationReport>(this.baseUrl, { params });
  }
}
