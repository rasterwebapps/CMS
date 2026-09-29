import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { Page } from './timetable.model';
import {
  AffectedSession,
  FacultyAbsence,
  FacultyAbsenceListFilter,
  FacultyAbsenceListItem,
  FacultyAbsenceRequest,
  SubstituteCandidate,
} from './faculty-absence.model';

@Injectable({ providedIn: 'root' })
export class FacultyAbsenceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/faculty-absences`;

  getPage(
    filter: FacultyAbsenceListFilter,
    page: number, size: number, sort: string, direction: 'asc' | 'desc',
  ): Observable<Page<FacultyAbsenceListItem>> {
    const params: Record<string, string> = { page: String(page), size: String(size), sort: `${sort},${direction}` };
    if (filter.fromDate) params['fromDate'] = filter.fromDate;
    if (filter.toDate) params['toDate'] = filter.toDate;
    if (filter.facultyName) params['facultyName'] = filter.facultyName;
    if (filter.substituteApplied !== null && filter.substituteApplied !== undefined) {
      params['substituteApplied'] = String(filter.substituteApplied);
    }
    return this.http.get<Page<FacultyAbsenceListItem>>(`${this.baseUrl}/page`, { params });
  }

  markAbsent(request: FacultyAbsenceRequest): Observable<FacultyAbsence> {
    return this.http.post<FacultyAbsence>(this.baseUrl, request);
  }

  getAbsence(absenceId: number): Observable<FacultyAbsence> {
    return this.http.get<FacultyAbsence>(`${this.baseUrl}/${absenceId}`);
  }

  getAffectedSessions(absenceId: number): Observable<AffectedSession[]> {
    return this.http.get<AffectedSession[]>(`${this.baseUrl}/${absenceId}/affected-sessions`);
  }

  getSubstituteCandidates(classScheduleId: number, date: string): Observable<SubstituteCandidate[]> {
    return this.http.get<SubstituteCandidate[]>(`${this.baseUrl}/sessions/${classScheduleId}/substitute-candidates`, {
      params: { date },
    });
  }

  applySubstitute(absenceId: number, classScheduleId: number, substituteFacultyId: number): Observable<AffectedSession> {
    return this.http.post<AffectedSession>(
      `${this.baseUrl}/${absenceId}/sessions/${classScheduleId}/apply-substitute`,
      { substituteFacultyId },
    );
  }
}
