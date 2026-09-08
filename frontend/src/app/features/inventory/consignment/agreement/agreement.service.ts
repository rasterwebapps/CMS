import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import { ConsignmentAgreement, ConsignmentAgreementRequest, Page } from './agreement.model';

@Injectable({ providedIn: 'root' })
export class ConsignmentAgreementService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/consignment/agreements`;

  getPage(p: { supplierId?: number | null; locationId?: number | null; activeOnly?: boolean | null; page?: number; size?: number }): Observable<Page<ConsignmentAgreement>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.supplierId != null) params = params.set('supplierId', p.supplierId);
    if (p.locationId != null) params = params.set('locationId', p.locationId);
    if (p.activeOnly) params = params.set('activeOnly', true);
    return this.http.get<Page<ConsignmentAgreement>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<ConsignmentAgreement> {
    return this.http.get<ConsignmentAgreement>(`${this.baseUrl}/${id}`);
  }

  getAllActive(): Observable<Page<ConsignmentAgreement>> {
    return this.getPage({ activeOnly: true, size: 500 });
  }

  create(request: ConsignmentAgreementRequest): Observable<ConsignmentAgreement> {
    return this.http.post<ConsignmentAgreement>(this.baseUrl, request);
  }

  update(id: number, request: ConsignmentAgreementRequest): Observable<ConsignmentAgreement> {
    return this.http.put<ConsignmentAgreement>(`${this.baseUrl}/${id}`, request);
  }
}
