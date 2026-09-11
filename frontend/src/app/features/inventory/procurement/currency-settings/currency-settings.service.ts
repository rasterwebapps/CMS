import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import { InventoryCurrencySettings, InventoryCurrencySettingsRequest } from './currency-settings.model';

@Injectable({ providedIn: 'root' })
export class CurrencySettingsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/procurement/currency-settings`;

  find(): Observable<InventoryCurrencySettings> {
    return this.http.get<InventoryCurrencySettings>(this.baseUrl);
  }

  save(request: InventoryCurrencySettingsRequest): Observable<InventoryCurrencySettings> {
    return this.http.put<InventoryCurrencySettings>(this.baseUrl, request);
  }
}
