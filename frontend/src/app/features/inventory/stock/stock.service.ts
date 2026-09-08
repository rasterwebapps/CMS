import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import { Page, StockBalance, StockMovementRequest, StockMovementResponse } from './stock.model';

@Injectable({ providedIn: 'root' })
export class StockService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/stock`;

  getBalancePage(p: { productId?: number | null; locationId?: number | null; page?: number; size?: number }): Observable<Page<StockBalance>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.productId != null) params = params.set('productId', p.productId);
    if (p.locationId != null) params = params.set('locationId', p.locationId);
    return this.http.get<Page<StockBalance>>(`${this.baseUrl}/balances/page`, { params });
  }

  recordMovement(request: StockMovementRequest): Observable<StockMovementResponse> {
    return this.http.post<StockMovementResponse>(`${this.baseUrl}/movements`, request);
  }
}
