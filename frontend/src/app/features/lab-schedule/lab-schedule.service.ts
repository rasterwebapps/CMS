import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  LabSchedule,
  LabScheduleRequest,
  LabSlot,
  LabSlotRequest,
} from './lab-schedule.model';

@Injectable({
  providedIn: 'root',
})
export class LabScheduleService {
  private readonly http = inject(HttpClient);
  private readonly scheduleUrl = `${environment.apiUrl}/lab-schedules`;
  private readonly slotUrl = `${environment.apiUrl}/lab-slots`;

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

  getAllSlots(): Observable<LabSlot[]> {
    return this.http.get<LabSlot[]>(this.slotUrl);
  }

  getSlotById(id: number): Observable<LabSlot> {
    return this.http.get<LabSlot>(`${this.slotUrl}/${id}`);
  }

  createSlot(request: LabSlotRequest): Observable<LabSlot> {
    return this.http.post<LabSlot>(this.slotUrl, request);
  }

  updateSlot(id: number, request: LabSlotRequest): Observable<LabSlot> {
    return this.http.put<LabSlot>(`${this.slotUrl}/${id}`, request);
  }

  deleteSlot(id: number): Observable<void> {
    return this.http.delete<void>(`${this.slotUrl}/${id}`);
  }
}
