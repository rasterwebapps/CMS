import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { Program, ProgramRequest } from './program.model';

@Injectable({
  providedIn: 'root',
})
export class ProgramService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/programs`;

  getAll(): Observable<Program[]> {
    return this.http.get<Program[]>(this.baseUrl);
  }

  getById(id: number): Observable<Program> {
    return this.http.get<Program>(`${this.baseUrl}/${id}`);
  }

  getByDepartment(departmentId: number): Observable<Program[]> {
    return this.http.get<Program[]>(`${this.baseUrl}/department/${departmentId}`);
  }

  create(request: ProgramRequest): Observable<Program> {
    return this.http.post<Program>(this.baseUrl, request);
  }

  update(id: number, request: ProgramRequest): Observable<Program> {
    return this.http.put<Program>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
