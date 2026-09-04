import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { Batch, BatchLifecycleImpact, BatchRequest, BatchStudent } from './batch.model';

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

  update(id: number, request: BatchRequest): Observable<Batch> {
    return this.http.put<Batch>(`${this.baseUrl}/${id}`, request);
  }

  /** A real delete, not a soft flag — the backend hard-blocks this whenever the batch still has
   *  students or timetable data, so by the time it succeeds the row is gone for good. */
  deleteBatch(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getLifecycleImpact(id: number): Observable<BatchLifecycleImpact> {
    return this.http.get<BatchLifecycleImpact>(`${this.baseUrl}/${id}/lifecycle-impact`);
  }

  nameExists(value: string, courseOfferingId: number, excludeId: number | null): Observable<boolean> {
    const params: Record<string, string> = { value, courseOfferingId: courseOfferingId.toString() };
    if (excludeId != null) params['excludeId'] = excludeId.toString();
    return this.http.get<boolean>(`${this.baseUrl}/name-exists`, { params });
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
