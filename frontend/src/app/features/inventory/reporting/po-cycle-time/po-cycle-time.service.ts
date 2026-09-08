import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import { PurchaseOrderCycleTimeReport } from './po-cycle-time.model';

@Injectable({ providedIn: 'root' })
export class PurchaseOrderCycleTimeReportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/reporting/purchase-order-cycle-time`;

  get(): Observable<PurchaseOrderCycleTimeReport> {
    return this.http.get<PurchaseOrderCycleTimeReport>(this.baseUrl);
  }
}
