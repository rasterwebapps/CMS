import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  AdmissionRequest,
  AdmissionResponse,
  AcademicQualificationRequest,
  AcademicQualificationResponse,
  AdmissionDocumentResponse,
} from './admission.model';

@Injectable({ providedIn: 'root' })
export class AdmissionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/admissions`;

  getAll(): Observable<AdmissionResponse[]> {
    return this.http.get<AdmissionResponse[]>(this.baseUrl);
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

  updateStatus(id: number, status: string): Observable<AdmissionResponse> {
    return this.http.patch<AdmissionResponse>(`${this.baseUrl}/${id}/status`, null, {
      params: new HttpParams().set('status', status),
    });
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

  getDocumentChecklist(admissionId: number): Observable<Record<string, string>> {
    return this.http.get<Record<string, string>>(`${this.baseUrl}/${admissionId}/documents/checklist`);
  }
}
