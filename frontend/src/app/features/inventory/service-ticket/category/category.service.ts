import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import {
  ActiveStatusUpdateRequest,
  ActiveStatusUpdateResponse,
  Page,
  ServiceTicketCategory,
  ServiceTicketCategoryRequest,
} from './category.model';

@Injectable({ providedIn: 'root' })
export class ServiceTicketCategoryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/ticket/categories`;

  getAll(activeOnly = false): Observable<ServiceTicketCategory[]> {
    const params = new HttpParams().set('activeOnly', activeOnly);
    return this.http.get<ServiceTicketCategory[]>(this.baseUrl, { params });
  }

  getPage(p: { search?: string; page?: number; size?: number }): Observable<Page<ServiceTicketCategory>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    return this.http.get<Page<ServiceTicketCategory>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<ServiceTicketCategory> {
    return this.http.get<ServiceTicketCategory>(`${this.baseUrl}/${id}`);
  }

  create(request: ServiceTicketCategoryRequest): Observable<ServiceTicketCategory> {
    return this.http.post<ServiceTicketCategory>(this.baseUrl, request);
  }

  update(id: number, request: ServiceTicketCategoryRequest): Observable<ServiceTicketCategory> {
    return this.http.put<ServiceTicketCategory>(`${this.baseUrl}/${id}`, request);
  }

  updateStatus(id: number, request: ActiveStatusUpdateRequest): Observable<ActiveStatusUpdateResponse> {
    return this.http.patch<ActiveStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  checkNameExists(value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/name-exists`, { params });
  }
}
