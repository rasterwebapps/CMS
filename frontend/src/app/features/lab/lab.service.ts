import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Lab,
  LabRequest,
  LabInChargeAssignment,
  LabInChargeAssignmentRequest,
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

  getById(id: number): Observable<Lab> {
    return this.http.get<Lab>(`${this.baseUrl}/${id}`);
  }

  getByDepartment(departmentId: number): Observable<Lab[]> {
    return this.http.get<Lab[]>(`${this.baseUrl}/department/${departmentId}`);
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
