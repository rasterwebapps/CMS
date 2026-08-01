import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { Batch, BatchAutoCreateRequest, BatchRequest, BatchStudent } from './batch.model';

@Injectable({ providedIn: 'root' })
export class BatchService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/batches`;

  getByCourseOffering(courseOfferingId: number): Observable<Batch[]> {
    return this.http.get<Batch[]>(this.baseUrl, {
      params: { courseOfferingId: courseOfferingId.toString() }
    });
  }

  getBySubjectAndTerm(subjectId: number, termInstanceId: number): Observable<Batch[]> {
    return this.http.get<Batch[]>(this.baseUrl, {
      params: { subjectId: subjectId.toString(), termInstanceId: termInstanceId.toString() }
    });
  }

  create(request: BatchRequest): Observable<Batch> {
    return this.http.post<Batch>(this.baseUrl, request);
  }

  autoCreate(request: BatchAutoCreateRequest): Observable<Batch[]> {
    return this.http.post<Batch[]>(`${this.baseUrl}/auto-create`, request);
  }

  update(id: number, request: BatchRequest): Observable<Batch> {
    return this.http.put<Batch>(`${this.baseUrl}/${id}`, request);
  }

  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getRoster(id: number): Observable<BatchStudent[]> {
    return this.http.get<BatchStudent[]>(`${this.baseUrl}/${id}/roster`);
  }

  addStudent(batchId: number, studentId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${batchId}/students/${studentId}`, null);
  }

  removeStudent(batchId: number, studentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${batchId}/students/${studentId}`);
  }
}
