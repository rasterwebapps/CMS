import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LabUtilizationReport, AttendanceAnalyticsReport } from './reports.model';

@Injectable({
  providedIn: 'root',
})
export class ReportsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/reports`;

  getLabUtilization(): Observable<LabUtilizationReport> {
    return this.http.get<LabUtilizationReport>(`${this.baseUrl}/lab-utilization`);
  }

  getAttendanceAnalytics(): Observable<AttendanceAnalyticsReport> {
    return this.http.get<AttendanceAnalyticsReport>(`${this.baseUrl}/attendance-analytics`);
  }

  getStudentPerformance(studentId: number): Observable<unknown> {
    return this.http.get(`${this.baseUrl}/student-performance?studentId=${studentId}`);
  }
}
