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
  FacultyDocumentReviewFilter,
  Page,
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

  getPage(p: {
    search?: string;
    specialityId?: number;
    status?: FacultyStatus;
    documentReview?: FacultyDocumentReviewFilter;
    page?: number;
    size?: number;
    sort?: string;
    direction?: 'asc' | 'desc';
  }): Observable<Page<Faculty>> {
    let params = new HttpParams()
      .set('page', p.page ?? 0)
      .set('size', p.size ?? 25);
    if (p.search)           params = params.set('search', p.search);
    if (p.specialityId != null) params = params.set('specialityId', p.specialityId.toString());
    if (p.status)           params = params.set('status', p.status);
    if (p.documentReview && p.documentReview !== 'ALL')
                            params = params.set('documentReview', p.documentReview);
    if (p.sort)             params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<Faculty>>(`${this.baseUrl}/page`, { params });
  }

  getById(id: number): Observable<Faculty> {
    return this.http.get<Faculty>(`${this.baseUrl}/${id}`);
  }

  getBySpecialityId(specialityId: number): Observable<Faculty[]> {
    const params = new HttpParams().set('specialityId', specialityId.toString());
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

  nrtsNumberExists(value: string, excludeId?: number): Observable<boolean> {
    let params = new HttpParams().set('value', value);
    if (excludeId != null) params = params.set('excludeId', excludeId.toString());
    return this.http.get<boolean>(`${this.baseUrl}/nrts-exists`, { params });
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
    force = false,
  ): Observable<FacultyDocument> {
    const formData = new FormData();
    formData.append('documentType', documentType);
    formData.append('file', file);
    if (remarks) formData.append('remarks', remarks);
    if (force) formData.append('force', 'true');
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

  exportFaculty(
    format: 'excel' | 'pdf',
    filters: { search?: string | null; specialityId?: number | null; status?: FacultyStatus | null; documentReview?: string | null } = {},
  ): Observable<Blob> {
    let params = new HttpParams().set('format', format);
    if (filters.search)         params = params.set('search', filters.search);
    if (filters.specialityId != null) params = params.set('specialityId', filters.specialityId.toString());
    if (filters.status)         params = params.set('status', filters.status);
    if (filters.documentReview) params = params.set('documentReview', filters.documentReview);
    return this.http.get(`${this.baseUrl}/export`, { params, responseType: 'blob' });
  }
}
