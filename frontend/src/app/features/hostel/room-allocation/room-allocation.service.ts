import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import {
  HostelRoomOccupancy,
  Page,
  RoomAllocation,
  RoomAllocationRequest,
  RoomAllocationStatus,
} from './room-allocation.model';

@Injectable({ providedIn: 'root' })
export class RoomAllocationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/room-allocations`;

  getOccupancy(): Observable<HostelRoomOccupancy[]> {
    return this.http.get<HostelRoomOccupancy[]>(`${this.baseUrl}/occupancy`);
  }

  getPage(p: { search?: string; status?: RoomAllocationStatus; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<RoomAllocation>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.status) params = params.set('status', p.status);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'desc'}`);
    return this.http.get<Page<RoomAllocation>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<RoomAllocation> {
    return this.http.get<RoomAllocation>(`${this.baseUrl}/${id}`);
  }

  getByStudentId(studentId: number): Observable<RoomAllocation[]> {
    return this.http.get<RoomAllocation[]>(`${this.baseUrl}/student/${studentId}`);
  }

  create(request: RoomAllocationRequest): Observable<RoomAllocation> {
    return this.http.post<RoomAllocation>(this.baseUrl, request);
  }

  updateStatus(id: number, status: RoomAllocationStatus): Observable<RoomAllocation> {
    return this.http.patch<RoomAllocation>(`${this.baseUrl}/${id}/status`, { status });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
