import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import {
  Page,
  ReturnableGoodsReceiptLine,
  SupplierReturn,
  SupplierReturnAddLineRequest,
  SupplierReturnCreateRequest,
  SupplierReturnLine,
} from './supplier-return.model';

@Injectable({ providedIn: 'root' })
export class SupplierReturnService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/receiving/supplier-returns`;

  getPage(p: { goodsReceiptId?: number | null; status?: string | null; page?: number; size?: number }): Observable<Page<SupplierReturn>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.goodsReceiptId != null) params = params.set('goodsReceiptId', p.goodsReceiptId);
    if (p.status) params = params.set('status', p.status);
    return this.http.get<Page<SupplierReturn>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<SupplierReturn> {
    return this.http.get<SupplierReturn>(`${this.baseUrl}/${id}`);
  }

  create(request: SupplierReturnCreateRequest): Observable<SupplierReturn> {
    return this.http.post<SupplierReturn>(this.baseUrl, request);
  }

  getReturnableLines(goodsReceiptId: number): Observable<ReturnableGoodsReceiptLine[]> {
    const params = new HttpParams().set('goodsReceiptId', goodsReceiptId);
    return this.http.get<ReturnableGoodsReceiptLine[]>(`${this.baseUrl}/returnable-lines`, { params });
  }

  addLine(returnId: number, request: SupplierReturnAddLineRequest): Observable<SupplierReturnLine> {
    return this.http.post<SupplierReturnLine>(`${this.baseUrl}/${returnId}/lines`, request);
  }

  removeLine(returnId: number, lineId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${returnId}/lines/${lineId}`);
  }

  complete(returnId: number): Observable<SupplierReturn> {
    return this.http.post<SupplierReturn>(`${this.baseUrl}/${returnId}/complete`, {});
  }

  cancel(returnId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${returnId}/cancel`, {});
  }
}
