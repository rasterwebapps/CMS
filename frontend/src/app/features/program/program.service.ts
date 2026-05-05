import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { DocumentTypeInfo, Program, ProgramRequest } from './program.model';

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

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  /** Returns the document types currently required by the program. */
  getRequiredDocumentTypes(programId: number): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/${programId}/document-types`);
  }

  /** Replaces the program's required document types with the supplied set. */
  setRequiredDocumentTypes(programId: number, types: string[]): Observable<string[]> {
    return this.http.put<string[]>(`${this.baseUrl}/${programId}/document-types`, types);
  }

  /** Returns the full catalogue of available document types (with display labels). */
  getAllDocumentTypes(): Observable<DocumentTypeInfo[]> {
    return this.http.get<DocumentTypeInfo[]>(this.documentTypesUrl);
  }
}
