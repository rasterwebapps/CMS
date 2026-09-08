import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments';
import {
  AssetMaintenanceMarkPerformedRequest,
  AssetMaintenanceSchedule,
  AssetMaintenanceScheduleRequest,
  Page,
} from './maintenance-schedule.model';

@Injectable({ providedIn: 'root' })
export class MaintenanceScheduleService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory/asset/maintenance-schedules`;

  getPage(p: { assetId?: number | null; overdueOnly?: boolean | null; activeOnly?: boolean | null; page?: number; size?: number }): Observable<Page<AssetMaintenanceSchedule>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.assetId != null) params = params.set('assetId', p.assetId);
    if (p.overdueOnly) params = params.set('overdueOnly', true);
    if (p.activeOnly) params = params.set('activeOnly', true);
    return this.http.get<Page<AssetMaintenanceSchedule>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<AssetMaintenanceSchedule> {
    return this.http.get<AssetMaintenanceSchedule>(`${this.baseUrl}/${id}`);
  }

  create(request: AssetMaintenanceScheduleRequest): Observable<AssetMaintenanceSchedule> {
    return this.http.post<AssetMaintenanceSchedule>(this.baseUrl, request);
  }

  update(id: number, request: AssetMaintenanceScheduleRequest): Observable<AssetMaintenanceSchedule> {
    return this.http.put<AssetMaintenanceSchedule>(`${this.baseUrl}/${id}`, request);
  }

  markPerformed(id: number, request: AssetMaintenanceMarkPerformedRequest): Observable<AssetMaintenanceSchedule> {
    return this.http.post<AssetMaintenanceSchedule>(`${this.baseUrl}/${id}/mark-performed`, request);
  }
}
