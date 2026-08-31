import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  Page,
  Subject,
  SubjectRequest,
  SubjectStatusUpdateRequest,
  SubjectStatusUpdateResponse,
} from './subject.model';

@Injectable({
  providedIn: 'root',
})
export class SubjectService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/subjects`;

  getAll(activeOnly = false): Observable<Subject[]> {
    return this.http.get<Subject[]>(`${this.baseUrl}?activeOnly=${activeOnly}`);
  }

  getPage(p: { search?: string; courseId?: number | null; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<Subject>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.courseId != null) params = params.set('courseId', p.courseId);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<Subject>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<Subject> {
    return this.http.get<Subject>(`${this.baseUrl}/${id}`);
  }

  create(request: SubjectRequest): Observable<Subject> {
    return this.http.post<Subject>(this.baseUrl, request);
  }

  update(id: number, request: SubjectRequest): Observable<Subject> {
    return this.http.put<Subject>(`${this.baseUrl}/${id}`, request);
  }

  updateStatus(id: number, request: SubjectStatusUpdateRequest): Observable<SubjectStatusUpdateResponse> {
    return this.http.patch<SubjectStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  /** Additive-only — adds `venueId` to every listed subject's eligible-venue set without touching
   *  any other field. Used by the Lab/Clinical venue-create forms to auto-link a freshly created
   *  venue back to the subjects a capacity checklist flagged as stuck on an over-capacity venue
   *  (see `linkSubjectIds` query param on `/labs/new` and `/clinical-venues/new`). */
  addEligibleVenue(subjectIds: number[], venueType: 'LAB' | 'CLINICAL', venueId: number): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/eligible-venues`, { subjectIds, venueType, venueId });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  checkNameExists(value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/name-exists`, { params });
  }

  checkCodeExists(value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/code-exists`, { params });
  }
}
