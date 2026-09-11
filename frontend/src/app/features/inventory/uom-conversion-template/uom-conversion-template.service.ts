import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import {
  UomConversionTemplate,
  UomConversionTemplateRequest,
  UomConversionTemplateStatusUpdateRequest,
  UomConversionTemplateStatusUpdateResponse,
  Page,
} from './uom-conversion-template.model';

@Injectable({ providedIn: 'root' })
export class UomConversionTemplateService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/uom-conversion-templates`;

  getAll(activeOnly = false): Observable<UomConversionTemplate[]> {
    const params = new HttpParams().set('activeOnly', activeOnly);
    return this.http.get<UomConversionTemplate[]>(this.baseUrl, { params });
  }

  /** Templates whose own base unit matches `baseUomId` — the "apply a template" picker embedded
   *  in a product's Unit Hierarchy section uses this so it only offers templates that can apply. */
  getApplicableTo(baseUomId: number): Observable<UomConversionTemplate[]> {
    const params = new HttpParams().set('baseUomId', baseUomId);
    return this.http.get<UomConversionTemplate[]>(this.baseUrl, { params });
  }

  getPage(p: { search?: string; page?: number; size?: number }): Observable<Page<UomConversionTemplate>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    return this.http.get<Page<UomConversionTemplate>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<UomConversionTemplate> {
    return this.http.get<UomConversionTemplate>(`${this.baseUrl}/${id}`);
  }

  create(request: UomConversionTemplateRequest): Observable<UomConversionTemplate> {
    return this.http.post<UomConversionTemplate>(this.baseUrl, request);
  }

  update(id: number, request: UomConversionTemplateRequest): Observable<UomConversionTemplate> {
    return this.http.put<UomConversionTemplate>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(id: number, request: UomConversionTemplateStatusUpdateRequest): Observable<UomConversionTemplateStatusUpdateResponse> {
    return this.http.patch<UomConversionTemplateStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  checkNameExists(value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/name-exists`, { params });
  }
}
