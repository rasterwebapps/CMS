import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import { InventoryDashboard } from './dashboard.model';

@Injectable({ providedIn: 'root' })
export class InventoryDashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/reporting/dashboard`;

  get(): Observable<InventoryDashboard> {
    return this.http.get<InventoryDashboard>(this.baseUrl);
  }
}
