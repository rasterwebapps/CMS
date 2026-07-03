import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments';
import { NumberSeriesDefinition } from './number-series-definition.model';

@Injectable({ providedIn: 'root' })
export class NumberSeriesDefinitionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/number-series`;

  getAll(): Observable<NumberSeriesDefinition[]> {
    return this.http.get<NumberSeriesDefinition[]>(this.baseUrl);
  }

  getById(id: number): Observable<NumberSeriesDefinition> {
    return this.http.get<NumberSeriesDefinition>(`${this.baseUrl}/${id}`);
  }

  preview(id: number): Observable<string> {
    return this.http.get(`${this.baseUrl}/${id}/preview`, { responseType: 'text' });
  }

  create(req: Partial<NumberSeriesDefinition>): Observable<NumberSeriesDefinition> {
    return this.http.post<NumberSeriesDefinition>(this.baseUrl, req);
  }

  update(id: number, req: Partial<NumberSeriesDefinition>): Observable<NumberSeriesDefinition> {
    return this.http.put<NumberSeriesDefinition>(`${this.baseUrl}/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
