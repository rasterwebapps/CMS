import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  Faculty,
  FacultyDocument,
  FacultyDocumentTypeRequirement,
  FacultyDocumentTypeRequirementRequest,
  FacultyRequest,
  FacultyStatus,
} from './faculty.model';

@Injectable({
  providedIn: 'root',
})
export class FacultyService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/faculty`;
  private readonly requirementsUrl = `${environment.apiUrl}/faculty-document-type-requirements`;

  getAll(): Observable<Faculty[]> {
    return this.http.get<Faculty[]>(this.baseUrl);
  }

  getById(id: number): Observable<Faculty> {
    return this.http.get<Faculty>(`${this.baseUrl}/${id}`);
  }

  getByDepartmentId(departmentId: number): Observable<Faculty[]> {
    const params = new HttpParams().set('departmentId', departmentId.toString());
    return this.http.get<Faculty[]>(this.baseUrl, { params });
  }

  getByStatus(status: FacultyStatus): Observable<Faculty[]> {
    const params = new HttpParams().set('status', status);
    return this.http.get<Faculty[]>(this.baseUrl, { params });
  }

  create(request: FacultyRequest): Observable<Faculty> {
    return this.http.post<Faculty>(this.baseUrl, request);
  }

  update(id: number, request: FacultyRequest): Observable<Faculty> {
    return this.http.put<Faculty>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  // ── Faculty Document Type Requirements (config) ───────────────
  getDocumentTypeRequirements(): Observable<FacultyDocumentTypeRequirement[]> {
    return this.http.get<FacultyDocumentTypeRequirement[]>(this.requirementsUrl);
  }

  createDocumentTypeRequirement(
    request: FacultyDocumentTypeRequirementRequest,
  ): Observable<FacultyDocumentTypeRequirement> {
    return this.http.post<FacultyDocumentTypeRequirement>(this.requirementsUrl, request);
  }

  deleteDocumentTypeRequirement(id: number): Observable<void> {
    return this.http.delete<void>(`${this.requirementsUrl}/${id}`);
  }

  getRequiredDocumentTypesForFaculty(facultyId: number): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/${facultyId}/documents/required-types`);
  }

  // ── Faculty Documents ─────────────────────────────────────────
  getDocuments(facultyId: number): Observable<FacultyDocument[]> {
    return this.http.get<FacultyDocument[]>(`${this.baseUrl}/${facultyId}/documents`);
  }

  updateDocumentStatus(
    facultyId: number,
    documentId: number,
    documentType: string,
    status: string,
    remarks?: string,
  ): Observable<FacultyDocument> {
    return this.http.put<FacultyDocument>(`${this.baseUrl}/${facultyId}/documents/${documentId}`, {
      documentType,
      status,
      remarks,
    });
  }

  uploadDocument(
    facultyId: number,
    documentType: string,
    file: File,
    remarks?: string,
  ): Observable<FacultyDocument> {
    const formData = new FormData();
    formData.append('documentType', documentType);
    formData.append('file', file);
    if (remarks) formData.append('remarks', remarks);
    return this.http.post<FacultyDocument>(
      `${this.baseUrl}/${facultyId}/documents/upload`,
      formData,
    );
  }

  deleteDocument(facultyId: number, documentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${facultyId}/documents/${documentId}`);
  }

  documentDownloadUrl(facultyId: number, documentId: number): string {
    return `${this.baseUrl}/${facultyId}/documents/${documentId}/download`;
  }

  /**
   * Download/view a document via authenticated HTTP (the JWT is attached by
   * `authInterceptor`). Plain `<a href>` links don't carry the bearer token and
   * therefore fail with 401 against the API.
   */
  downloadDocumentBlob(facultyId: number, documentId: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.baseUrl}/${facultyId}/documents/${documentId}/download`, {
      observe: 'response',
      responseType: 'blob',
    });
  }
}
