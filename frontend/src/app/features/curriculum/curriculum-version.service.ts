import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  CurriculumVersion,
  CurriculumVersionRequest,
  CurriculumSemesterCourse,
  CurriculumSemesterCourseRequest,
  CurriculumFullView,
} from './curriculum-version.model';

@Injectable({ providedIn: 'root' })
export class CurriculumVersionService {
  private readonly http = inject(HttpClient);

  getByProgram(programId: number): Observable<CurriculumVersion[]> {
    return this.http.get<CurriculumVersion[]>(`${environment.apiUrl}/curriculum-versions`, {
      params: { programId: programId.toString() }
    });
  }

  getById(id: number): Observable<CurriculumVersion> {
    return this.http.get<CurriculumVersion>(`${environment.apiUrl}/curriculum-versions/${id}`);
  }

  create(request: CurriculumVersionRequest): Observable<CurriculumVersion> {
    return this.http.post<CurriculumVersion>(`${environment.apiUrl}/curriculum-versions`, request);
  }

  update(id: number, request: CurriculumVersionRequest): Observable<CurriculumVersion> {
    return this.http.put<CurriculumVersion>(`${environment.apiUrl}/curriculum-versions/${id}`, request);
  }

  clone(id: number, newVersionName: string, newEffectiveAcademicYearId: number): Observable<CurriculumVersion> {
    return this.http.post<CurriculumVersion>(
      `${environment.apiUrl}/curriculum-versions/${id}/clone`,
      null,
      { params: { newVersionName, newEffectiveAcademicYearId: newEffectiveAcademicYearId.toString() } }
    );
  }

  getFullCurriculum(curriculumVersionId: number): Observable<CurriculumFullView> {
    return this.http.get<CurriculumFullView>(`${environment.apiUrl}/curriculum-semester-courses`, {
      params: { curriculumVersionId: curriculumVersionId.toString() }
    });
  }

  addCourse(request: CurriculumSemesterCourseRequest): Observable<CurriculumSemesterCourse> {
    return this.http.post<CurriculumSemesterCourse>(`${environment.apiUrl}/curriculum-semester-courses`, request);
  }

  removeCourse(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/curriculum-semester-courses/${id}`);
  }
}
