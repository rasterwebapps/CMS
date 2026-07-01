import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { Equipment, EquipmentRequest, Page } from './equipment.model';

@Injectable({
  providedIn: 'root',
})
export class EquipmentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/equipment`;

  getAll(): Observable<Equipment[]> {
    return this.http.get<Equipment[]>(this.baseUrl);
  }

  getPage(p: { search?: string; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<Equipment>> {
    let params = new HttpParams()
      .set('page', p.page ?? 0)
      .set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.sort)   params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<Equipment>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<Equipment> {
    return this.http.get<Equipment>(`${this.baseUrl}/${id}`);
  }

  create(request: EquipmentRequest): Observable<Equipment> {
    return this.http.post<Equipment>(this.baseUrl, request);
  }

  update(id: number, request: EquipmentRequest): Observable<Equipment> {
    return this.http.put<Equipment>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
