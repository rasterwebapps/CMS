import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  AcademicYear,
  AcademicYearRequest,
  Semester,
  SemesterRequest,
} from './academic-year.model';

@Injectable({
  providedIn: 'root',
})
export class AcademicYearService {
  private readonly http = inject(HttpClient);
  private readonly academicYearUrl = `${environment.apiUrl}/academic-years`;
  private readonly semesterUrl = `${environment.apiUrl}/semesters`;

  // Academic Year methods
  getAllAcademicYears(): Observable<AcademicYear[]> {
    return this.http.get<AcademicYear[]>(this.academicYearUrl);
  }

  getAcademicYearById(id: number): Observable<AcademicYear> {
    return this.http.get<AcademicYear>(`${this.academicYearUrl}/${id}`);
  }

  getCurrentAcademicYear(): Observable<AcademicYear> {
    return this.http.get<AcademicYear>(`${this.academicYearUrl}/current`);
  }

  createAcademicYear(request: AcademicYearRequest): Observable<AcademicYear> {
    return this.http.post<AcademicYear>(this.academicYearUrl, request);
  }

  updateAcademicYear(id: number, request: AcademicYearRequest): Observable<AcademicYear> {
    return this.http.put<AcademicYear>(`${this.academicYearUrl}/${id}`, request);
  }

  deleteAcademicYear(id: number): Observable<void> {
    return this.http.delete<void>(`${this.academicYearUrl}/${id}`);
  }

  // Semester methods
  getAllSemesters(): Observable<Semester[]> {
    return this.http.get<Semester[]>(this.semesterUrl);
  }

  getSemesterById(id: number): Observable<Semester> {
    return this.http.get<Semester>(`${this.semesterUrl}/${id}`);
  }

  getSemestersByAcademicYear(academicYearId: number): Observable<Semester[]> {
    return this.http.get<Semester[]>(`${this.semesterUrl}/academic-year/${academicYearId}`);
  }

  createSemester(request: SemesterRequest): Observable<Semester> {
    return this.http.post<Semester>(this.semesterUrl, request);
  }

  updateSemester(id: number, request: SemesterRequest): Observable<Semester> {
    return this.http.put<Semester>(`${this.semesterUrl}/${id}`, request);
  }

  deleteSemester(id: number): Observable<void> {
    return this.http.delete<void>(`${this.semesterUrl}/${id}`);
  }
}
