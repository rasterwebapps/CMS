import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { Examination, ExaminationRequest, ExamResult, ExamResultRequest } from './examination.model';

@Injectable({
  providedIn: 'root',
})
export class ExaminationService {
  private readonly http = inject(HttpClient);
  private readonly examinationUrl = `${environment.apiUrl}/examinations`;
  private readonly resultUrl = `${environment.apiUrl}/exam-results`;

  getAll(): Observable<Examination[]> {
    return this.http.get<Examination[]>(this.examinationUrl);
  }

  getBySubject(subjectId: number): Observable<Examination[]> {
    return this.http.get<Examination[]>(`${this.examinationUrl}/subject/${subjectId}`);
  }

  getById(id: number): Observable<Examination> {
    return this.http.get<Examination>(`${this.examinationUrl}/${id}`);
  }

  create(request: ExaminationRequest): Observable<Examination> {
    return this.http.post<Examination>(this.examinationUrl, request);
  }

  update(id: number, request: ExaminationRequest): Observable<Examination> {
    return this.http.put<Examination>(`${this.examinationUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.examinationUrl}/${id}`);
  }

  getResults(examinationId: number): Observable<ExamResult[]> {
    return this.http.get<ExamResult[]>(`${this.resultUrl}/examination/${examinationId}`);
  }

  getMyResults(): Observable<ExamResult[]> {
    return this.http.get<ExamResult[]>(`${this.resultUrl}/my`);
  }

  /** A guardian's linked ward's exam results -- backend re-validates ward ownership server-side. */
  getWardResults(studentId: number): Observable<ExamResult[]> {
    return this.http.get<ExamResult[]>(`${this.resultUrl}/my-wards?studentId=${studentId}`);
  }

  createResult(request: ExamResultRequest): Observable<ExamResult> {
    return this.http.post<ExamResult>(this.resultUrl, request);
  }

  updateResult(id: number, request: ExamResultRequest): Observable<ExamResult> {
    return this.http.put<ExamResult>(`${this.resultUrl}/${id}`, request);
  }
}
