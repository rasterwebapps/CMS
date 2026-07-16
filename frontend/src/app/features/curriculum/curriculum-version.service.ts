import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  CurriculumVersion,
  CurriculumVersionRequest,
  CurriculumSemesterCourse,
  CurriculumSemesterCourseRequest,
  CurriculumFullView,
  CurriculumElectiveGroup,
  CurriculumElectiveGroupRequest,
  AttendanceThreshold,
  AttendanceThresholdRequest,
  Page,
} from './curriculum-version.model';

@Injectable({ providedIn: 'root' })
export class CurriculumVersionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/curriculum-versions`;

  getByProgram(programId: number): Observable<CurriculumVersion[]> {
    return this.http.get<CurriculumVersion[]>(this.baseUrl, {
      params: { programId: programId.toString() }
    });
  }

  getPage(p: {
    search?: string; programId?: number | null; isActive?: boolean | null;
    page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc';
  }): Observable<Page<CurriculumVersion>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.programId != null) params = params.set('programId', p.programId);
    if (p.isActive != null) params = params.set('isActive', p.isActive);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<CurriculumVersion>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<CurriculumVersion> {
    return this.http.get<CurriculumVersion>(`${this.baseUrl}/${id}`);
  }

  create(request: CurriculumVersionRequest): Observable<CurriculumVersion> {
    return this.http.post<CurriculumVersion>(this.baseUrl, request);
  }

  update(id: number, request: CurriculumVersionRequest): Observable<CurriculumVersion> {
    return this.http.put<CurriculumVersion>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  clone(id: number, newVersionName: string, newEffectiveAcademicYearId: number): Observable<CurriculumVersion> {
    return this.http.post<CurriculumVersion>(
      `${this.baseUrl}/${id}/clone`,
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

  updateCourse(id: number, request: CurriculumSemesterCourseRequest): Observable<CurriculumSemesterCourse> {
    return this.http.put<CurriculumSemesterCourse>(`${environment.apiUrl}/curriculum-semester-courses/${id}`, request);
  }

  removeCourse(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/curriculum-semester-courses/${id}`);
  }

  getElectiveGroups(curriculumVersionId: number, termNumber: number): Observable<CurriculumElectiveGroup[]> {
    return this.http.get<CurriculumElectiveGroup[]>(`${environment.apiUrl}/curriculum-elective-groups`, {
      params: { curriculumVersionId: curriculumVersionId.toString(), termNumber: termNumber.toString() }
    });
  }

  createElectiveGroup(request: CurriculumElectiveGroupRequest): Observable<CurriculumElectiveGroup> {
    return this.http.post<CurriculumElectiveGroup>(`${environment.apiUrl}/curriculum-elective-groups`, request);
  }

  deleteElectiveGroup(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/curriculum-elective-groups/${id}`);
  }

  getAttendanceThresholds(curriculumTermCourseId: number): Observable<AttendanceThreshold[]> {
    return this.http.get<AttendanceThreshold[]>(`${environment.apiUrl}/attendance-thresholds`, {
      params: { curriculumTermCourseId: curriculumTermCourseId.toString() }
    });
  }

  upsertAttendanceThreshold(request: AttendanceThresholdRequest): Observable<AttendanceThreshold> {
    return this.http.put<AttendanceThreshold>(`${environment.apiUrl}/attendance-thresholds`, request);
  }

  deleteAttendanceThreshold(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/attendance-thresholds/${id}`);
  }
}
