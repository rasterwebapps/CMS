import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SystemConfiguration, SystemConfigurationRequest } from './settings.model';

@Injectable({
  providedIn: 'root',
})
export class SettingsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/system-configurations`;

  getAll(): Observable<SystemConfiguration[]> {
    return this.http.get<SystemConfiguration[]>(this.baseUrl);
  }

  getById(id: number): Observable<SystemConfiguration> {
    return this.http.get<SystemConfiguration>(`${this.baseUrl}/${id}`);
  }

  getByCategory(category: string): Observable<SystemConfiguration[]> {
    return this.http.get<SystemConfiguration[]>(`${this.baseUrl}/category/${category}`);
  }

  getByKey(key: string): Observable<SystemConfiguration> {
    return this.http.get<SystemConfiguration>(`${this.baseUrl}/key/${key}`);
  }

  create(request: SystemConfigurationRequest): Observable<SystemConfiguration> {
    return this.http.post<SystemConfiguration>(this.baseUrl, request);
  }

  update(id: number, request: SystemConfigurationRequest): Observable<SystemConfiguration> {
    return this.http.put<SystemConfiguration>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
