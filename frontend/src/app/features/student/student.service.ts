import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  CourseRegistration,
  Page,
  ProgramTransferAnalysis,
  ProgramTransferRecord,
  ProgramTransferRequest,
  Student,
  StudentExplorerParams,
  StudentFeeLedger,
  StudentRequest,
  StudentTermEnrollment,
} from './student.model';

@Injectable({
  providedIn: 'root',
})
export class StudentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/students`;

  getAll(): Observable<Student[]> {
    return this.http.get<Student[]>(this.baseUrl);
  }

  getExplorer(p: StudentExplorerParams): Observable<Page<Student>> {
    let params = new HttpParams()
      .set('page', p.page ?? 0)
      .set('size', p.size ?? 25);
    if (p.sort)           params = params.set('sort', p.sort);
    if (p.programId)      params = params.set('programId', p.programId);
    if (p.courseId)       params = params.set('courseId', p.courseId);
    if (p.academicYearId) params = params.set('academicYearId', p.academicYearId);
    if (p.status)         params = params.set('status', p.status);
    if (p.studentType)    params = params.set('studentType', p.studentType);
    if (p.search && p.search.length >= 3) params = params.set('search', p.search);
    return this.http.get<Page<Student>>(`${this.baseUrl}/explorer`, { params });
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
    return this.http.get<StudentTermEnrollment[]>(
      `${environment.apiUrl}/student-term-enrollments?studentId=${studentId}`,
    );
  }

  getRegistrationsByEnrollment(enrollmentId: number): Observable<CourseRegistration[]> {
    return this.http.get<CourseRegistration[]>(
      `${environment.apiUrl}/course-registrations?enrollmentId=${enrollmentId}`,
    );
  }

  getStudentFeeLedger(studentId: number): Observable<StudentFeeLedger> {
    return this.http.get<StudentFeeLedger>(
      `${environment.apiUrl}/fee-reports/student-ledger?studentId=${studentId}`,
    );
  }

  analyzeProgramTransfer(studentId: number, newProgramId: number): Observable<ProgramTransferAnalysis> {
    return this.http.get<ProgramTransferAnalysis>(
      `${this.baseUrl}/${studentId}/program-transfer-analysis`,
      { params: { newProgramId: newProgramId.toString() } },
    );
  }

  executeProgramTransfer(studentId: number, request: ProgramTransferRequest): Observable<ProgramTransferRecord> {
    return this.http.post<ProgramTransferRecord>(
      `${this.baseUrl}/${studentId}/program-transfer`,
      request,
    );
  }

  getTransferHistory(studentId: number): Observable<ProgramTransferRecord[]> {
    return this.http.get<ProgramTransferRecord[]>(`${this.baseUrl}/${studentId}/program-transfers`);
  }

  exportStudents(
    format: 'excel' | 'pdf',
    filters: {
      programId?: number | null;
      courseId?: number | null;
      academicYearId?: number | null;
      status?: string | null;
      studentType?: string | null;
      search?: string | null;
    } = {},
  ): Observable<Blob> {
    let params = new HttpParams().set('format', format);
    if (filters.programId)      params = params.set('programId', filters.programId);
    if (filters.courseId)       params = params.set('courseId', filters.courseId);
    if (filters.academicYearId) params = params.set('academicYearId', filters.academicYearId);
    if (filters.status)         params = params.set('status', filters.status);
    if (filters.studentType)    params = params.set('studentType', filters.studentType);
    if (filters.search && filters.search.length >= 3) params = params.set('search', filters.search);
    return this.http.get(`${this.baseUrl}/export`, { params, responseType: 'blob' });
  }
}
