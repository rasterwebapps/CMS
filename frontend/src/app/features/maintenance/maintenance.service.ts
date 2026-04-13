import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MaintenanceRequest, MaintenanceRequestDto } from './maintenance.model';

@Injectable({
  providedIn: 'root',
})
export class MaintenanceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/maintenance`;

  getAll(): Observable<MaintenanceRequest[]> {
    return this.http.get<MaintenanceRequest[]>(this.baseUrl);
  }

  getById(id: number): Observable<MaintenanceRequest> {
    return this.http.get<MaintenanceRequest>(`${this.baseUrl}/${id}`);
  }

  create(request: MaintenanceRequestDto): Observable<MaintenanceRequest> {
    return this.http.post<MaintenanceRequest>(this.baseUrl, request);
  }

  update(id: number, request: MaintenanceRequestDto): Observable<MaintenanceRequest> {
    return this.http.put<MaintenanceRequest>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
