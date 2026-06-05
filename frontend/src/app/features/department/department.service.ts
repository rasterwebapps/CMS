import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { Speciality, SpecialityRequest } from './department.model';

@Injectable({
  providedIn: 'root',
})
export class SpecialityService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/specialities`;

  getAll(): Observable<Speciality[]> {
    return this.http.get<Speciality[]>(this.baseUrl);
  }

  getById(id: number): Observable<Speciality> {
    return this.http.get<Speciality>(`${this.baseUrl}/${id}`);
  }

  create(request: SpecialityRequest): Observable<Speciality> {
    return this.http.post<Speciality>(this.baseUrl, request);
  }

  update(id: number, request: SpecialityRequest): Observable<Speciality> {
    return this.http.put<Speciality>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
