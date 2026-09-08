import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import {
  Page,
  StockTransfer,
  StockTransferAddLineRequest,
  StockTransferCreateRequest,
  StockTransferLine,
} from './stock-transfer.model';

@Injectable({ providedIn: 'root' })
export class StockTransferService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/stock/transfers`;

  getPage(p: { locationId?: number | null; status?: string | null; page?: number; size?: number }): Observable<Page<StockTransfer>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.locationId != null) params = params.set('locationId', p.locationId);
    if (p.status) params = params.set('status', p.status);
    return this.http.get<Page<StockTransfer>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<StockTransfer> {
    return this.http.get<StockTransfer>(`${this.baseUrl}/${id}`);
  }

  create(request: StockTransferCreateRequest): Observable<StockTransfer> {
    return this.http.post<StockTransfer>(this.baseUrl, request);
  }

  addLine(transferId: number, request: StockTransferAddLineRequest): Observable<StockTransferLine> {
    return this.http.post<StockTransferLine>(`${this.baseUrl}/${transferId}/lines`, request);
  }

  removeLine(transferId: number, lineId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${transferId}/lines/${lineId}`);
  }

  complete(transferId: number): Observable<StockTransfer> {
    return this.http.post<StockTransfer>(`${this.baseUrl}/${transferId}/complete`, {});
  }

  cancel(transferId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${transferId}/cancel`, {});
  }
}
