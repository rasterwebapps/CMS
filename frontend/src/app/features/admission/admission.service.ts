import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  AdmissionExplorerParams,
  AdmissionRequest,
  AdmissionResponse,
  AcademicQualificationRequest,
  AcademicQualificationResponse,
  AdmissionDocumentResponse,
  Page,
} from './admission.model';

@Injectable({ providedIn: 'root' })
export class AdmissionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/admissions`;

  getAll(): Observable<AdmissionResponse[]> {
    return this.http.get<AdmissionResponse[]>(this.baseUrl);
  }

  getExplorer(p: AdmissionExplorerParams): Observable<Page<AdmissionResponse>> {
    let params = new HttpParams()
      .set('page', p.page ?? 0)
      .set('size', p.size ?? 25);
    if (p.sort)          params = params.set('sort', p.sort);
    if (p.programId)     params = params.set('programId', p.programId);
    if (p.courseId)      params = params.set('courseId', p.courseId);
    if (p.academicYearId) params = params.set('academicYearId', p.academicYearId);
    if (p.status)        params = params.set('status', p.status);
    if (p.studentType)   params = params.set('studentType', p.studentType);
    if (p.search && p.search.length >= 3) params = params.set('search', p.search);
    return this.http.get<Page<AdmissionResponse>>(`${this.baseUrl}/explorer`, { params });
  }

  getById(id: number): Observable<AdmissionResponse> {
    return this.http.get<AdmissionResponse>(`${this.baseUrl}/${id}`);
  }

  getByStudent(studentId: number): Observable<AdmissionResponse> {
    return this.http.get<AdmissionResponse>(`${this.baseUrl}/student/${studentId}`);
  }

  create(request: AdmissionRequest): Observable<AdmissionResponse> {
    return this.http.post<AdmissionResponse>(this.baseUrl, request);
  }

  update(id: number, request: AdmissionRequest): Observable<AdmissionResponse> {
    return this.http.put<AdmissionResponse>(`${this.baseUrl}/${id}`, request);
  }


  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  addQualification(admissionId: number, request: AcademicQualificationRequest): Observable<AcademicQualificationResponse> {
    return this.http.post<AcademicQualificationResponse>(`${this.baseUrl}/${admissionId}/qualifications`, request);
  }

  getQualifications(admissionId: number): Observable<AcademicQualificationResponse[]> {
    return this.http.get<AcademicQualificationResponse[]>(`${this.baseUrl}/${admissionId}/qualifications`);
  }

  deleteQualification(qualificationId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/qualifications/${qualificationId}`);
  }

  getDocuments(admissionId: number): Observable<AdmissionDocumentResponse[]> {
    return this.http.get<AdmissionDocumentResponse[]>(`${this.baseUrl}/${admissionId}/documents`);
  }

  verifyDocument(documentId: number, status: string, verifiedBy: string): Observable<AdmissionDocumentResponse> {
    return this.http.patch<AdmissionDocumentResponse>(`${this.baseUrl}/documents/${documentId}/verify`, null, {
      params: new HttpParams().set('status', status).set('verifiedBy', verifiedBy),
    });
  }

  getDocumentChecklist(admissionId: number): Observable<{ mandatory: Record<string, string>; optional: Record<string, string> }> {
    return this.http.get<{ mandatory: Record<string, string>; optional: Record<string, string> }>(`${this.baseUrl}/${admissionId}/documents/checklist`);
  }

  uploadDocument(
    admissionId: number,
    documentType: string,
    file: File,
    remarks?: string,
    force = false,
  ): Observable<AdmissionDocumentResponse> {
    const formData = new FormData();
    formData.append('documentType', documentType);
    formData.append('file', file);
    if (remarks) formData.append('remarks', remarks);
    if (force) formData.append('force', 'true');
    return this.http.post<AdmissionDocumentResponse>(
      `${this.baseUrl}/${admissionId}/documents/upload`,
      formData,
    );
  }

  downloadDocumentBlob(documentId: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.baseUrl}/documents/${documentId}/download`, {
      observe: 'response',
      responseType: 'blob',
    });
  }

  exportAdmissions(
    format: 'excel' | 'pdf',
    filters: {
      programId?: number | null;
      courseId?: number | null;
      academicYearId?: number | null;
      status?: string | null;
      studentType?: string | null;
      search?: string | null;
      sort?: string | null;
      direction?: string | null;
    } = {},
  ): Observable<Blob> {
    let params = new HttpParams().set('format', format);
    if (filters.programId)      params = params.set('programId', filters.programId);
    if (filters.courseId)       params = params.set('courseId', filters.courseId);
    if (filters.academicYearId) params = params.set('academicYearId', filters.academicYearId);
    if (filters.status)         params = params.set('status', filters.status);
    if (filters.studentType)    params = params.set('studentType', filters.studentType);
    if (filters.search && filters.search.length >= 3) params = params.set('search', filters.search);
    if (filters.sort)           params = params.set('sort', filters.sort);
    if (filters.direction)      params = params.set('direction', filters.direction);
    return this.http.get(`${this.baseUrl}/export`, { params, responseType: 'blob' });
  }
}
