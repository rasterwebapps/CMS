import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import {
  ConsignmentStockConsumeRequest,
  ConsignmentStockLine,
  ConsignmentStockReceiveRequest,
  Page,
} from './stock-line.model';

@Injectable({ providedIn: 'root' })
export class ConsignmentStockLineService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/consignment/stock-lines`;

  getPage(p: { agreementId?: number | null; productId?: number | null; page?: number; size?: number }): Observable<Page<ConsignmentStockLine>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.agreementId != null) params = params.set('agreementId', p.agreementId);
    if (p.productId != null) params = params.set('productId', p.productId);
    return this.http.get<Page<ConsignmentStockLine>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<ConsignmentStockLine> {
    return this.http.get<ConsignmentStockLine>(`${this.baseUrl}/${id}`);
  }

  receive(request: ConsignmentStockReceiveRequest): Observable<ConsignmentStockLine> {
    return this.http.post<ConsignmentStockLine>(`${this.baseUrl}/receive`, request);
  }

  consume(id: number, request: ConsignmentStockConsumeRequest): Observable<ConsignmentStockLine> {
    return this.http.post<ConsignmentStockLine>(`${this.baseUrl}/${id}/consume`, request);
  }
}
