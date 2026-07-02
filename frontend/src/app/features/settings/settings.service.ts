import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page, SystemConfiguration, SystemConfigurationRequest } from './settings.model';

@Injectable({
  providedIn: 'root',
})
export class SettingsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/system-configurations`;

  getAll(): Observable<SystemConfiguration[]> {
    return this.http.get<SystemConfiguration[]>(this.baseUrl);
  }

  getPage(p: { search?: string; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<SystemConfiguration>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<SystemConfiguration>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<SystemConfiguration> {
    return this.http.get<SystemConfiguration>(`${this.baseUrl}/${id}`);
  }

  getByCategory(category: string): Observable<SystemConfiguration[]> {
    return this.http.get<SystemConfiguration[]>(`${this.baseUrl}?category=${encodeURIComponent(category)}`);
  }

  getByKey(key: string): Observable<SystemConfiguration> {
    return this.http.get<SystemConfiguration>(`${this.baseUrl}/key/${key}`);
  }

  create(request: SystemConfigurationRequest): Observable<SystemConfiguration> {
    return this.http.post<SystemConfiguration>(this.baseUrl, request);
  }

  upsert(request: SystemConfigurationRequest): Observable<SystemConfiguration> {
    return this.http.put<SystemConfiguration>(`${this.baseUrl}/upsert`, request);
  }

  update(id: number, request: SystemConfigurationRequest): Observable<SystemConfiguration> {
    return this.http.put<SystemConfiguration>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
