import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Equipment, EquipmentRequest } from './equipment.model';

@Injectable({
  providedIn: 'root',
})
export class EquipmentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/equipment`;

  getAll(): Observable<Equipment[]> {
    return this.http.get<Equipment[]>(this.baseUrl);
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
