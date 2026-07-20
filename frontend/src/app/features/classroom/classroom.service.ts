import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  Classroom,
  ClassroomRequest,
  ClassroomStatusUpdateRequest,
  ClassroomStatusUpdateResponse,
  Page,
} from './classroom.model';

@Injectable({ providedIn: 'root' })
export class ClassroomService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/classrooms`;

  getAll(activeOnly = false): Observable<Classroom[]> {
    return this.http.get<Classroom[]>(`${this.baseUrl}?activeOnly=${activeOnly}`);
  }

  getPage(p: { search?: string; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<Classroom>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<Classroom>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<Classroom> {
    return this.http.get<Classroom>(`${this.baseUrl}/${id}`);
  }

  create(request: ClassroomRequest): Observable<Classroom> {
    return this.http.post<Classroom>(this.baseUrl, request);
  }

  update(id: number, request: ClassroomRequest): Observable<Classroom> {
    return this.http.put<Classroom>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(
    id: number,
    request: ClassroomStatusUpdateRequest,
  ): Observable<ClassroomStatusUpdateResponse> {
    return this.http.patch<ClassroomStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  checkNameExists(value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/name-exists`, { params });
  }
}
