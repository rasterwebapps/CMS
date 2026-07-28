import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  LogProgressRequest,
  OfferingProgress,
  SessionOccurrence,
  SyllabusUnitOption,
  TermProgressSummary,
} from './progress-tracking.model';

@Injectable({ providedIn: 'root' })
export class ProgressTrackingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/progress-tracking`;

  getAvailableUnits(classScheduleId: number): Observable<SyllabusUnitOption[]> {
    return this.http.get<SyllabusUnitOption[]>(`${this.baseUrl}/sessions/${classScheduleId}/units`);
  }

  getLoggableOccurrenceDates(classScheduleId: number, from: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/sessions/${classScheduleId}/occurrence-dates`, {
      params: { from },
    });
  }

  getOccurrence(classScheduleId: number, date: string): Observable<SessionOccurrence> {
    return this.http.get<SessionOccurrence>(`${this.baseUrl}/sessions/${classScheduleId}/occurrences/${date}`);
  }

  logCoverage(request: LogProgressRequest): Observable<SessionOccurrence> {
    return this.http.post<SessionOccurrence>(`${this.baseUrl}/log`, request);
  }

  getOfferingProgress(courseOfferingId: number): Observable<OfferingProgress> {
    return this.http.get<OfferingProgress>(`${this.baseUrl}/course-offerings/${courseOfferingId}`);
  }

  getTermProgressSummary(termInstanceId: number): Observable<TermProgressSummary> {
    return this.http.get<TermProgressSummary>(`${this.baseUrl}/term-instances/${termInstanceId}/summary`);
  }
}
