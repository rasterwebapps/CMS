import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { Attendance, AttendanceRequest, AttendanceReport, BulkAttendanceRequest } from './attendance.model';

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

  getByCourse(courseId: number): Observable<Attendance[]> {
    return this.http.get<Attendance[]>(`${this.baseUrl}?courseId=${courseId}`);
  }

  getReports(courseId?: number): Observable<AttendanceReport[]> {
    const url = courseId
      ? `${this.baseUrl}/reports?courseId=${courseId}`
      : `${this.baseUrl}/reports`;
    return this.http.get<AttendanceReport[]>(url);
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
