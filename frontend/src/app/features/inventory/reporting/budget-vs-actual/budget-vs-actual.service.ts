import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import { BudgetVsActualReport } from './budget-vs-actual.model';

@Injectable({ providedIn: 'root' })
export class BudgetVsActualReportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/reporting/budget-vs-actual`;

  get(): Observable<BudgetVsActualReport> {
    return this.http.get<BudgetVsActualReport>(this.baseUrl);
  }
}
