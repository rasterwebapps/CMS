import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  Lab,
  LabRequest,
  LabInChargeAssignment,
  LabInChargeAssignmentRequest,
  Page,
} from './lab.model';

@Injectable({
  providedIn: 'root',
})
export class LabService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/labs`;

  getAll(): Observable<Lab[]> {
    return this.http.get<Lab[]>(this.baseUrl);
  }

  getPage(p: { search?: string; specialityId?: number | null; labType?: string | null; status?: string | null; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<Lab>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.specialityId != null) params = params.set('specialityId', p.specialityId);
    if (p.labType != null) params = params.set('labType', p.labType);
    if (p.status != null) params = params.set('status', p.status);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<Lab>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<Lab> {
    return this.http.get<Lab>(`${this.baseUrl}/${id}`);
  }

  getBySpeciality(specialityId: number): Observable<Lab[]> {
    return this.http.get<Lab[]>(`${this.baseUrl}/speciality/${specialityId}`);
  }

  create(request: LabRequest): Observable<Lab> {
    return this.http.post<Lab>(this.baseUrl, request);
  }

  update(id: number, request: LabRequest): Observable<Lab> {
    return this.http.put<Lab>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getAssignments(labId: number): Observable<LabInChargeAssignment[]> {
    return this.http.get<LabInChargeAssignment[]>(`${this.baseUrl}/${labId}/assignments`);
  }

  assignInCharge(labId: number, request: LabInChargeAssignmentRequest): Observable<LabInChargeAssignment> {
    return this.http.post<LabInChargeAssignment>(`${this.baseUrl}/${labId}/assign`, request);
  }

  removeAssignment(labId: number, assignmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${labId}/assignments/${assignmentId}`);
  }
}
