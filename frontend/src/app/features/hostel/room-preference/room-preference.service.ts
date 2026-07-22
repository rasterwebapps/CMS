import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../../environments';
import { Page, RoomPreference, RoomPreferenceRequest, RoomPreferenceStatus } from './room-preference.model';

@Injectable({ providedIn: 'root' })
export class RoomPreferenceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/room-preferences`;

  getPage(p: { search?: string; status?: RoomPreferenceStatus; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<RoomPreference>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.status) params = params.set('status', p.status);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'desc'}`);
    return this.http.get<Page<RoomPreference>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<RoomPreference> {
    return this.http.get<RoomPreference>(`${this.baseUrl}/${id}`);
  }

  /** Returns null (rather than erroring) when no preference exists yet — the normal state for a
   *  brand-new enquiry/student, not a failure. */
  getByEnquiryId(enquiryId: number): Observable<RoomPreference | null> {
    return this.http.get<RoomPreference>(`${this.baseUrl}/enquiry/${enquiryId}`).pipe(
      catchError(() => of(null)),
    );
  }

  getByStudentId(studentId: number): Observable<RoomPreference | null> {
    return this.http.get<RoomPreference>(`${this.baseUrl}/student/${studentId}`).pipe(
      catchError(() => of(null)),
    );
  }

  create(request: RoomPreferenceRequest): Observable<RoomPreference> {
    return this.http.post<RoomPreference>(this.baseUrl, request);
  }

  update(id: number, request: RoomPreferenceRequest): Observable<RoomPreference> {
    return this.http.put<RoomPreference>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
