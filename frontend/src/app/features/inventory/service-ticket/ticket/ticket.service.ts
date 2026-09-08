import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import {
  Page,
  ServiceTicket,
  ServiceTicketAssignRequest,
  ServiceTicketCancelRequest,
  ServiceTicketCloseRequest,
  ServiceTicketCreateRequest,
  ServiceTicketResolveRequest,
} from './ticket.model';

@Injectable({ providedIn: 'root' })
export class ServiceTicketService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/ticket/tickets`;

  getPage(p: { locationId?: number | null; categoryId?: number | null; status?: string | null; priority?: string | null; page?: number; size?: number }): Observable<Page<ServiceTicket>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.locationId != null) params = params.set('locationId', p.locationId);
    if (p.categoryId != null) params = params.set('categoryId', p.categoryId);
    if (p.status) params = params.set('status', p.status);
    if (p.priority) params = params.set('priority', p.priority);
    return this.http.get<Page<ServiceTicket>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<ServiceTicket> {
    return this.http.get<ServiceTicket>(`${this.baseUrl}/${id}`);
  }

  create(request: ServiceTicketCreateRequest): Observable<ServiceTicket> {
    return this.http.post<ServiceTicket>(this.baseUrl, request);
  }

  assign(id: number, request: ServiceTicketAssignRequest): Observable<ServiceTicket> {
    return this.http.post<ServiceTicket>(`${this.baseUrl}/${id}/assign`, request);
  }

  resolve(id: number, request: ServiceTicketResolveRequest): Observable<ServiceTicket> {
    return this.http.post<ServiceTicket>(`${this.baseUrl}/${id}/resolve`, request);
  }

  close(id: number, request: ServiceTicketCloseRequest): Observable<ServiceTicket> {
    return this.http.post<ServiceTicket>(`${this.baseUrl}/${id}/close`, request);
  }

  cancel(id: number, request: ServiceTicketCancelRequest): Observable<ServiceTicket> {
    return this.http.post<ServiceTicket>(`${this.baseUrl}/${id}/cancel`, request);
  }
}
