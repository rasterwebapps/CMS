import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import { AssetDepreciationSummaryReport } from './asset-depreciation-summary.model';

@Injectable({ providedIn: 'root' })
export class AssetDepreciationSummaryReportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/reporting/asset-depreciation-summary`;

  get(): Observable<AssetDepreciationSummaryReport> {
    return this.http.get<AssetDepreciationSummaryReport>(this.baseUrl);
  }
}
