import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import { TaxType } from './tax-type.model';

/**
 * Minimal read-side client for Tax Types -- backs the Tax Type picker on the Tax Rule form
 * (`GET /inventory/procurement/tax-types` was built for exactly this but had never been wired
 * up on the frontend). No dedicated Tax Types management screen exists yet; the backend's full
 * CRUD there is unused beyond the one seeded "GST" row until that's built as its own feature.
 */
@Injectable({ providedIn: 'root' })
export class TaxTypeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/procurement/tax-types`;

  getAll(activeOnly = false): Observable<TaxType[]> {
    const params = new HttpParams().set('activeOnly', activeOnly);
    return this.http.get<TaxType[]>(this.baseUrl, { params });
  }
}
