import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  Course,
  CourseRequest,
  CourseStatusUpdateRequest,
  CourseStatusUpdateResponse,
  Page,
} from './course.model';

@Injectable({
  providedIn: 'root',
})
export class CourseService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/courses`;

  getAll(): Observable<Course[]> {
    return this.http.get<Course[]>(this.baseUrl);
  }

  getPage(p: { search?: string; programId?: number | null; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<Course>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.programId != null) params = params.set('programId', p.programId);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<Course>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<Course> {
    return this.http.get<Course>(`${this.baseUrl}/${id}`);
  }

  getByProgram(programId: number): Observable<Course[]> {
    return this.http.get<Course[]>(`${this.baseUrl}/program/${programId}`);
  }

  create(request: CourseRequest): Observable<Course> {
    return this.http.post<Course>(this.baseUrl, request);
  }

  update(id: number, request: CourseRequest): Observable<Course> {
    return this.http.put<Course>(`${this.baseUrl}/${id}`, request);
  }

  updateStatus(id: number, request: CourseStatusUpdateRequest): Observable<CourseStatusUpdateResponse> {
    return this.http.patch<CourseStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
