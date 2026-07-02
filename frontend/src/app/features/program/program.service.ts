import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  DocumentRequirementsRequest,
  DocumentRequirementsResponse,
  DocumentTypeInfo,
  Page,
  Program,
  ProgramRequest,
  ProgramStatusUpdateRequest,
  ProgramStatusUpdateResponse,
} from './program.model';

@Injectable({
  providedIn: 'root',
})
export class ProgramService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/programs`;
  private readonly documentTypesUrl = `${environment.apiUrl}/document-types`;

  getAll(): Observable<Program[]> {
    return this.http.get<Program[]>(this.baseUrl);
  }

  getPage(p: { search?: string; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<Program>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<Program>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<Program> {
    return this.http.get<Program>(`${this.baseUrl}/${id}`);
  }

  create(request: ProgramRequest): Observable<Program> {
    return this.http.post<Program>(this.baseUrl, request);
  }

  update(id: number, request: ProgramRequest): Observable<Program> {
    return this.http.put<Program>(`${this.baseUrl}/${id}`, request);
  }

  updateStatus(id: number, request: ProgramStatusUpdateRequest): Observable<ProgramStatusUpdateResponse> {
    return this.http.patch<ProgramStatusUpdateResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  /** Returns mandatory and optional document type codes for the program. */
  getDocumentRequirements(programId: number): Observable<DocumentRequirementsResponse> {
    return this.http.get<DocumentRequirementsResponse>(`${this.baseUrl}/${programId}/document-types`);
  }

  /** Replaces the program's mandatory and optional document requirements. */
  setDocumentRequirements(programId: number, req: DocumentRequirementsRequest): Observable<DocumentRequirementsResponse> {
    return this.http.put<DocumentRequirementsResponse>(`${this.baseUrl}/${programId}/document-types`, req);
  }

  /** Returns the full catalogue of available document types (with display labels). */
  getAllDocumentTypes(): Observable<DocumentTypeInfo[]> {
    return this.http.get<DocumentTypeInfo[]>(this.documentTypesUrl);
  }
}
