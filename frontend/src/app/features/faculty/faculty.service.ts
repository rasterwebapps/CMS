import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Faculty, FacultyRequest, FacultyStatus } from './faculty.model';

@Injectable({
  providedIn: 'root',
})
export class FacultyService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/faculty`;

  getAll(): Observable<Faculty[]> {
    return this.http.get<Faculty[]>(this.baseUrl);
  }

  getById(id: number): Observable<Faculty> {
    return this.http.get<Faculty>(`${this.baseUrl}/${id}`);
  }

  getByDepartmentId(departmentId: number): Observable<Faculty[]> {
    const params = new HttpParams().set('departmentId', departmentId.toString());
    return this.http.get<Faculty[]>(this.baseUrl, { params });
  }

  getByStatus(status: FacultyStatus): Observable<Faculty[]> {
    const params = new HttpParams().set('status', status);
    return this.http.get<Faculty[]>(this.baseUrl, { params });
  }

  create(request: FacultyRequest): Observable<Faculty> {
    return this.http.post<Faculty>(this.baseUrl, request);
  }

  update(id: number, request: FacultyRequest): Observable<Faculty> {
    return this.http.put<Faculty>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
