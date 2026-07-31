import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  AffectedSession,
  FacultyAbsence,
  FacultyAbsenceRequest,
  SubstituteCandidate,
} from './faculty-absence.model';

@Injectable({ providedIn: 'root' })
export class FacultyAbsenceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/faculty-absences`;

  markAbsent(request: FacultyAbsenceRequest): Observable<FacultyAbsence> {
    return this.http.post<FacultyAbsence>(this.baseUrl, request);
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
