import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  LabSchedule,
  LabScheduleRequest,
  Page,
} from './lab-schedule.model';

export interface LabScheduleFilters {
  search?: string | null;
  labId?: number | null;
  facultyId?: number | null;
  termInstanceId?: number | null;
  dayOfWeek?: string | null;
  sessionType?: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class LabScheduleService {
  private readonly http = inject(HttpClient);
  private readonly scheduleUrl = `${environment.apiUrl}/lab-schedules`;

  getAll(): Observable<LabSchedule[]> {
    return this.http.get<LabSchedule[]>(this.scheduleUrl);
  }

  getByFacultyAndTerm(facultyId: number, termInstanceId: number): Observable<LabSchedule[]> {
    return this.http.get<LabSchedule[]>(this.scheduleUrl, {
      params: { facultyId: facultyId.toString(), termInstanceId: termInstanceId.toString() },
    });
  }

  getPage(filters: LabScheduleFilters, page: number, size: number, sort?: string): Observable<Page<LabSchedule>> {
    let params = new HttpParams().set('page', page).set('size', size);
    params = this.appendFilters(params, filters);
    if (sort) params = params.set('sort', sort);
    return this.http.get<Page<LabSchedule>>(`${this.scheduleUrl}/page`, { params });
  }

  export(format: 'excel' | 'pdf', filters: LabScheduleFilters, sort?: string, direction?: string): Observable<Blob> {
    let params = new HttpParams().set('format', format);
    params = this.appendFilters(params, filters);
    if (sort) params = params.set('sort', sort);
    if (direction) params = params.set('direction', direction);
    return this.http.get(`${this.scheduleUrl}/export`, { params, responseType: 'blob' });
  }

  private appendFilters(params: HttpParams, filters: LabScheduleFilters): HttpParams {
    if (filters.search)          params = params.set('search', filters.search);
    if (filters.labId != null)   params = params.set('labId', filters.labId);
    if (filters.facultyId != null) params = params.set('facultyId', filters.facultyId);
    if (filters.termInstanceId != null) params = params.set('termInstanceId', filters.termInstanceId);
    if (filters.dayOfWeek)       params = params.set('dayOfWeek', filters.dayOfWeek);
    if (filters.sessionType)     params = params.set('sessionType', filters.sessionType);
    return params;
  }

  getById(id: number): Observable<LabSchedule> {
    return this.http.get<LabSchedule>(`${this.scheduleUrl}/${id}`);
  }

  update(id: number, request: LabScheduleRequest): Observable<LabSchedule> {
    return this.http.put<LabSchedule>(`${this.scheduleUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.scheduleUrl}/${id}`);
  }
}
