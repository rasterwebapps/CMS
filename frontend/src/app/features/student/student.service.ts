import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { Student, StudentRequest } from './student.model';

@Injectable({
  providedIn: 'root',
})
export class StudentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/students`;

  getAll(): Observable<Student[]> {
    return this.http.get<Student[]>(this.baseUrl);
  }

  getAllByProgram(programId: number): Observable<Student[]> {
    return this.http.get<Student[]>(`${this.baseUrl}?programId=${programId}`);
  }

  getById(id: number): Observable<Student> {
    return this.http.get<Student>(`${this.baseUrl}/${id}`);
  }

  getByRollNumber(rollNumber: string): Observable<Student> {
    return this.http.get<Student>(`${this.baseUrl}/roll/${rollNumber}`);
  }

  create(request: StudentRequest): Observable<Student> {
    return this.http.post<Student>(this.baseUrl, request);
  }

  update(id: number, request: StudentRequest): Observable<Student> {
    return this.http.put<Student>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
