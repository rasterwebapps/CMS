import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { FacultyAvailabilityBlock, FacultyAvailabilityRequest } from './faculty-availability.model';

@Injectable({ providedIn: 'root' })
export class FacultyAvailabilityService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/faculty-availability`;

  getForFaculty(facultyId: number): Observable<FacultyAvailabilityBlock[]> {
    const params = new HttpParams().set('facultyId', facultyId);
    return this.http.get<FacultyAvailabilityBlock[]>(this.baseUrl, { params });
  }

  addBlock(request: FacultyAvailabilityRequest): Observable<FacultyAvailabilityBlock> {
    return this.http.post<FacultyAvailabilityBlock>(this.baseUrl, request);
  }

  removeBlock(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
