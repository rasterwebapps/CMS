import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { ClassSchedule, MyTimetableResponse, SwapCandidate, SwapTarget, TimetableActionResponse, TimetableGenerationResponse } from './timetable.model';

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

  getMyTimetable(termInstanceId: number, weekStart?: string): Observable<MyTimetableResponse> {
    let params = new HttpParams().set('termInstanceId', termInstanceId);
    if (weekStart) params = params.set('weekStart', weekStart);
    return this.http.get<MyTimetableResponse>(`${this.baseUrl}/me`, { params });
  }

  getSwapCandidates(termInstanceId: number, sessionId: number): Observable<SwapCandidate[]> {
    return this.http.get<SwapCandidate[]>(`${this.baseUrl}/${termInstanceId}/sessions/${sessionId}/swap-candidates`);
  }

  swapSession(termInstanceId: number, sessionId: number, target: SwapTarget): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${termInstanceId}/sessions/${sessionId}/swap`, target);
  }
}
