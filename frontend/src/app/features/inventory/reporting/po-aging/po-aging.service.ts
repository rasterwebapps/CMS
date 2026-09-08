import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import { PurchaseOrderAgingReport } from './po-aging.model';

@Injectable({ providedIn: 'root' })
export class PurchaseOrderAgingReportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/reporting/purchase-order-aging`;

  get(): Observable<PurchaseOrderAgingReport> {
    return this.http.get<PurchaseOrderAgingReport>(this.baseUrl);
  }
}
