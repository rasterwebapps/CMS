import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  LabSchedule,
  LabScheduleRequest,
} from './lab-schedule.model';

@Injectable({
  providedIn: 'root',
})
export class LabScheduleService {
  private readonly http = inject(HttpClient);
  private readonly scheduleUrl = `${environment.apiUrl}/lab-schedules`;

  getAll(): Observable<LabSchedule[]> {
    return this.http.get<LabSchedule[]>(this.scheduleUrl);
  }

  getById(id: number): Observable<LabSchedule> {
    return this.http.get<LabSchedule>(`${this.scheduleUrl}/${id}`);
  }

  create(request: LabScheduleRequest): Observable<LabSchedule> {
    return this.http.post<LabSchedule>(this.scheduleUrl, request);
  }

  update(id: number, request: LabScheduleRequest): Observable<LabSchedule> {
    return this.http.put<LabSchedule>(`${this.scheduleUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.scheduleUrl}/${id}`);
  }
}
