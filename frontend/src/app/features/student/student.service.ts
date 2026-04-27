import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { CourseRegistration, Student, StudentRequest, StudentTermEnrollment } from './student.model';

@Injectable({
  providedIn: 'root',
})
export class StudentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/students`;

  getAll(): Observable<Student[]> {
    return this.http.get<Student[]>(this.baseUrl);
  }

  getStudentsWithoutRollNumber(courseId?: number, programId?: number): Observable<Student[]> {
    let url = `${this.baseUrl}/without-roll-number`;
    const params: string[] = [];
    if (programId) params.push(`programId=${programId}`);
    if (courseId) params.push(`courseId=${courseId}`);
    if (params.length) url += `?${params.join('&')}`;
    return this.http.get<Student[]>(url);
  }

  assignRollNumber(id: number, rollNumber: string): Observable<Student> {
    return this.http.patch<Student>(`${this.baseUrl}/${id}/roll-number`, { rollNumber });
  }

  bulkAssignRollNumbers(assignments: { studentId: number; rollNumber: string }[]): Observable<Student[]> {
    return this.http.post<Student[]>(`${this.baseUrl}/bulk-assign-roll-numbers`, { assignments });
  }

  getAllByProgram(programId: number): Observable<Student[]> {
    return this.http.get<Student[]>(`${this.baseUrl}?programId=${programId}`);
  }

  getById(id: number): Observable<Student> {
    return this.http.get<Student>(`${this.baseUrl}/${id}`);
  }

  getByRollNumber(rollNumber: string): Observable<Student> {
    return this.http.get<Student>(`${this.baseUrl}/roll/${rollNumber}`);
  }

  create(request: StudentRequest): Observable<Student> {
    return this.http.post<Student>(this.baseUrl, request);
  }

  update(id: number, request: StudentRequest): Observable<Student> {
    return this.http.put<Student>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getEnrollmentsByStudent(studentId: number): Observable<StudentTermEnrollment[]> {
    const baseUrl = environment.apiUrl.replace('/api/v1', '');
    return this.http.get<StudentTermEnrollment[]>(
      `${baseUrl}/api/student-term-enrollments?studentId=${studentId}`,
    );
  }

  getRegistrationsByEnrollment(enrollmentId: number): Observable<CourseRegistration[]> {
    const baseUrl = environment.apiUrl.replace('/api/v1', '');
    return this.http.get<CourseRegistration[]>(
      `${baseUrl}/api/course-registrations?enrollmentId=${enrollmentId}`,
    );
  }
}
