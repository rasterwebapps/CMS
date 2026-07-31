import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import { StaffingAssignmentRequest, UnstaffedCell } from './staffing.model';

@Injectable({ providedIn: 'root' })
export class StaffingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/timetables/staffing`;

  getUnstaffedCells(termInstanceId: number): Observable<UnstaffedCell[]> {
    return this.http.get<UnstaffedCell[]>(this.baseUrl, {
      params: { termInstanceId: termInstanceId.toString() },
    });
  }

  staffCell(id: number, request: StaffingAssignmentRequest): Observable<UnstaffedCell> {
    return this.http.put<UnstaffedCell>(`${this.baseUrl}/cells/${id}`, request);
  }
}
