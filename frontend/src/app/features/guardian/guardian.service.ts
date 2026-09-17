import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { GuardianRequest, GuardianResponse, StudentGuardianResponse, WardSummaryResponse } from './guardian.model';

@Injectable({
  providedIn: 'root',
})
export class GuardianService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/guardians`;

  findAll(): Observable<GuardianResponse[]> {
    return this.http.get<GuardianResponse[]>(this.baseUrl);
  }

  create(request: GuardianRequest): Observable<GuardianResponse> {
    return this.http.post<GuardianResponse>(this.baseUrl, request);
  }

  linkWard(guardianId: number, studentId: number, isPrimary: boolean): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${guardianId}/wards/${studentId}?isPrimary=${isPrimary}`, {});
  }

  unlinkWard(guardianId: number, studentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${guardianId}/wards/${studentId}`);
  }

  /** Guardians currently linked to a given student -- powers the Student Detail Guardians tab. */
  findByStudent(studentId: number): Observable<StudentGuardianResponse[]> {
    return this.http.get<StudentGuardianResponse[]>(`${environment.apiUrl}/students/${studentId}/guardians`);
  }

  /** Current authenticated guardian's own wards -- powers the ward switcher. */
  myWards(): Observable<WardSummaryResponse[]> {
    return this.http.get<WardSummaryResponse[]>(`${environment.apiUrl}/guardian/wards`);
  }
}
