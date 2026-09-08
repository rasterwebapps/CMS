import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import {
  GoodsReceipt,
  GoodsReceiptAddLineRequest,
  GoodsReceiptCreateRequest,
  GoodsReceiptLine,
  Page,
  ReceivablePurchaseOrderLine,
} from './goods-receipt.model';

@Injectable({ providedIn: 'root' })
export class GoodsReceiptService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/receiving/goods-receipts`;

  getPage(p: { purchaseOrderId?: number | null; status?: string | null; page?: number; size?: number }): Observable<Page<GoodsReceipt>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.purchaseOrderId != null) params = params.set('purchaseOrderId', p.purchaseOrderId);
    if (p.status) params = params.set('status', p.status);
    return this.http.get<Page<GoodsReceipt>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<GoodsReceipt> {
    return this.http.get<GoodsReceipt>(`${this.baseUrl}/${id}`);
  }

  create(request: GoodsReceiptCreateRequest): Observable<GoodsReceipt> {
    return this.http.post<GoodsReceipt>(this.baseUrl, request);
  }

  getReceivableLines(purchaseOrderId: number): Observable<ReceivablePurchaseOrderLine[]> {
    const params = new HttpParams().set('purchaseOrderId', purchaseOrderId);
    return this.http.get<ReceivablePurchaseOrderLine[]>(`${this.baseUrl}/receivable-lines`, { params });
  }

  addLine(receiptId: number, request: GoodsReceiptAddLineRequest): Observable<GoodsReceiptLine> {
    return this.http.post<GoodsReceiptLine>(`${this.baseUrl}/${receiptId}/lines`, request);
  }

  removeLine(receiptId: number, lineId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${receiptId}/lines/${lineId}`);
  }

  confirm(receiptId: number): Observable<GoodsReceipt> {
    return this.http.post<GoodsReceipt>(`${this.baseUrl}/${receiptId}/confirm`, {});
  }
}
