import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { Attendance, AttendanceRequest, AttendanceReport, AvailableSubject, BulkAttendanceRequest } from './attendance.model';

@Injectable({
  providedIn: 'root',
})
export class AttendanceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/attendance`;

  getAll(): Observable<Attendance[]> {
    return this.http.get<Attendance[]>(this.baseUrl);
  }

  getByStudent(studentId: number): Observable<Attendance[]> {
    return this.http.get<Attendance[]>(`${this.baseUrl}?studentId=${studentId}`);
  }

  getBySubject(subjectId: number): Observable<Attendance[]> {
    return this.http.get<Attendance[]>(`${this.baseUrl}?subjectId=${subjectId}`);
  }

  getReports(studentId: number, subjectId: number): Observable<AttendanceReport[]> {
    return this.http.get<AttendanceReport[]>(`${this.baseUrl}/reports?studentId=${studentId}&subjectId=${subjectId}`);
  }

  /** Day-mapping- and blocked-period-aware: resolves through {@code DayMappingOverride} so a
   *  compensatory working day correctly offers the borrowed weekday's subjects. Faculty is
   *  resolved server-side from the authenticated caller (matches the `/timetables/me` pattern). */
  getAvailableSubjects(date: string): Observable<AvailableSubject[]> {
    return this.http.get<AvailableSubject[]>(`${this.baseUrl}/available-subjects?date=${date}`);
  }

  getSubjectRoster(subjectId: number): Observable<Array<{ id: number; fullName: string; rollNumber: string }>> {
    return this.http.get<Array<{ id: number; fullName: string; rollNumber: string }>>(
      `${this.baseUrl}/subject-roster?subjectId=${subjectId}`
    );
  }

  markBulk(request: BulkAttendanceRequest): Observable<Attendance[]> {
    return this.http.post<Attendance[]>(`${this.baseUrl}/bulk`, request);
  }

  create(request: AttendanceRequest): Observable<Attendance> {
    return this.http.post<Attendance>(this.baseUrl, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
