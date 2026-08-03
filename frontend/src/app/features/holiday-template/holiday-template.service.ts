import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { HolidayTemplate, HolidayTemplateRequest, Page } from './holiday-template.model';

@Injectable({ providedIn: 'root' })
export class HolidayTemplateService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/holiday-templates`;

  getAll(): Observable<HolidayTemplate[]> {
    return this.http.get<HolidayTemplate[]>(this.baseUrl);
  }

  getPage(p: { search?: string; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<HolidayTemplate>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<HolidayTemplate>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<HolidayTemplate> {
    return this.http.get<HolidayTemplate>(`${this.baseUrl}/${id}`);
  }

  create(request: HolidayTemplateRequest): Observable<HolidayTemplate> {
    return this.http.post<HolidayTemplate>(this.baseUrl, request);
  }

  update(id: number, request: HolidayTemplateRequest): Observable<HolidayTemplate> {
    return this.http.put<HolidayTemplate>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  checkNameExists(value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/name-exists`, { params });
  }
}
