import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  ClassSchedule,
  ClassScheduleOccurrence,
  MyTimetableResponse,
  ResourceGridRow,
  ResourceGridType,
  StaffSwapCandidate,
  SwapCandidate,
  SwapTarget,
  TimetableActionResponse,
  TimetableGenerationResponse,
  TimetableOccurrenceScope,
} from './timetable.model';

@Injectable({ providedIn: 'root' })
export class TimetableService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/timetables`;

  generate(termInstanceId: number): Observable<TimetableGenerationResponse> {
    const params = new HttpParams().set('termInstanceId', termInstanceId);
    return this.http.post<TimetableGenerationResponse>(`${this.baseUrl}/generate`, null, { params });
  }

  getDraft(termInstanceId: number): Observable<ClassSchedule[]> {
    const params = new HttpParams().set('termInstanceId', termInstanceId);
    return this.http.get<ClassSchedule[]>(`${this.baseUrl}/draft`, { params });
  }

  getPublished(termInstanceId: number): Observable<ClassSchedule[]> {
    const params = new HttpParams().set('termInstanceId', termInstanceId);
    return this.http.get<ClassSchedule[]>(this.baseUrl, { params });
  }

  approve(termInstanceId: number): Observable<TimetableActionResponse> {
    return this.http.post<TimetableActionResponse>(`${this.baseUrl}/${termInstanceId}/approve`, null);
  }

  clear(termInstanceId: number): Observable<TimetableActionResponse> {
    return this.http.delete<TimetableActionResponse>(`${this.baseUrl}/${termInstanceId}`);
  }

  revertToDraft(termInstanceId: number): Observable<TimetableActionResponse> {
    return this.http.post<TimetableActionResponse>(`${this.baseUrl}/${termInstanceId}/revert-to-draft`, null);
  }

  getOccurrences(
    termInstanceId: number, from: string, to: string, scope: TimetableOccurrenceScope,
  ): Observable<ClassScheduleOccurrence[]> {
    const params = new HttpParams()
      .set('termInstanceId', termInstanceId).set('from', from).set('to', to).set('scope', scope);
    return this.http.get<ClassScheduleOccurrence[]>(`${this.baseUrl}/occurrences`, { params });
  }

  getMyTimetable(termInstanceId: number, weekStart?: string): Observable<MyTimetableResponse> {
    let params = new HttpParams().set('termInstanceId', termInstanceId);
    if (weekStart) params = params.set('weekStart', weekStart);
    return this.http.get<MyTimetableResponse>(`${this.baseUrl}/me`, { params });
  }

  getResourceGrid(type: ResourceGridType, termInstanceId: number, dayOfWeek: string): Observable<ResourceGridRow[]> {
    const params = new HttpParams().set('termInstanceId', termInstanceId).set('dayOfWeek', dayOfWeek);
    const path = type === 'FACULTY' ? 'resource-grid/faculty' : 'resource-grid/classroom';
    return this.http.get<ResourceGridRow[]>(`${this.baseUrl}/${path}`, { params });
  }

  getStaffSwapCandidates(classScheduleId: number, date: string): Observable<StaffSwapCandidate[]> {
    const params = new HttpParams().set('date', date);
    return this.http.get<StaffSwapCandidate[]>(`${this.baseUrl}/staff-swap/sessions/${classScheduleId}/candidates`, { params });
  }

  applyStaffSwap(classScheduleId: number, targetClassScheduleId: number, date: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/staff-swap/sessions/${classScheduleId}/apply`, { targetClassScheduleId, date });
  }

  getSwapCandidates(termInstanceId: number, sessionId: number): Observable<SwapCandidate[]> {
    return this.http.get<SwapCandidate[]>(`${this.baseUrl}/${termInstanceId}/sessions/${sessionId}/swap-candidates`);
  }

  swapSession(termInstanceId: number, sessionId: number, target: SwapTarget): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${termInstanceId}/sessions/${sessionId}/swap`, target);
  }
}
