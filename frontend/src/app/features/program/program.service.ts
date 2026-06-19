import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  DocumentRequirementsRequest,
  DocumentRequirementsResponse,
  DocumentTypeInfo,
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
