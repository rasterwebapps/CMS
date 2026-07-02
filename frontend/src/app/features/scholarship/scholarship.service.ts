import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  DisbursementRequest,
  Page,
  ScholarshipApplication,
  ScholarshipApplicationRequest,
  ScholarshipApprovalRequest,
  ScholarshipDisbursement,
  ScholarshipEligibility,
  ScholarshipEligibilityRequest,
  ScholarshipRejectionRequest,
  ScholarshipType,
  ScholarshipTypeRequest,
  ScholarshipTypeStatusUpdateRequest,
  ScholarshipTypeStatusUpdateResponse,
} from './scholarship.model';

@Injectable({ providedIn: 'root' })
export class ScholarshipService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  getScholarshipTypes(): Observable<ScholarshipType[]> {
    return this.http.get<ScholarshipType[]>(`${this.baseUrl}/scholarships`);
  }

  getScholarshipTypesPage(p: { search?: string; page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' }): Observable<Page<ScholarshipType>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    if (p.sort) params = params.set('sort', `${p.sort},${p.direction ?? 'asc'}`);
    return this.http.get<Page<ScholarshipType>>(`${this.baseUrl}/scholarships/page`, { params });
  }

  getScholarshipType(id: number): Observable<ScholarshipType> {
    return this.http.get<ScholarshipType>(`${this.baseUrl}/scholarships/${id}`);
  }

  createScholarshipType(request: ScholarshipTypeRequest): Observable<ScholarshipType> {
    return this.http.post<ScholarshipType>(`${this.baseUrl}/scholarships`, request);
  }

  updateScholarshipType(id: number, request: ScholarshipTypeRequest): Observable<ScholarshipType> {
    return this.http.put<ScholarshipType>(`${this.baseUrl}/scholarships/${id}`, request);
  }

  deactivateScholarshipType(id: number): Observable<ScholarshipTypeStatusUpdateResponse> {
    return this.updateScholarshipTypeStatus(id, { isActive: false });
  }

  reactivateScholarshipType(id: number): Observable<ScholarshipTypeStatusUpdateResponse> {
    return this.updateScholarshipTypeStatus(id, { isActive: true });
  }

  updateScholarshipTypeStatus(
    id: number,
    request: ScholarshipTypeStatusUpdateRequest,
  ): Observable<ScholarshipTypeStatusUpdateResponse> {
    return this.http.patch<ScholarshipTypeStatusUpdateResponse>(
      `${this.baseUrl}/scholarships/${id}/status`,
      request,
    );
  }

  getEligibleScholarships(studentId: number): Observable<ScholarshipType[]> {
    return this.http.get<ScholarshipType[]>(`${this.baseUrl}/students/${studentId}/scholarships/eligible`);
  }

  getStudentScholarships(studentId: number): Observable<ScholarshipApplication[]> {
    return this.http.get<ScholarshipApplication[]>(`${this.baseUrl}/students/${studentId}/scholarships`);
  }

  apply(studentId: number, request: ScholarshipApplicationRequest): Observable<ScholarshipApplication> {
    return this.http.post<ScholarshipApplication>(`${this.baseUrl}/students/${studentId}/scholarships/apply`, request);
  }

  getEligibility(studentId: number): Observable<ScholarshipEligibility> {
    return this.http.get<ScholarshipEligibility>(`${this.baseUrl}/students/${studentId}/eligibility`);
  }

  updateEligibility(studentId: number, request: ScholarshipEligibilityRequest): Observable<ScholarshipEligibility> {
    return this.http.put<ScholarshipEligibility>(`${this.baseUrl}/students/${studentId}/eligibility`, request);
  }

  verifyEligibility(studentId: number, remarks?: string): Observable<ScholarshipEligibility> {
    return this.http.put<ScholarshipEligibility>(`${this.baseUrl}/students/${studentId}/eligibility/verify`, { remarks });
  }

  getStudentDisbursements(studentId: number): Observable<ScholarshipDisbursement[]> {
    return this.http.get<ScholarshipDisbursement[]>(`${this.baseUrl}/students/${studentId}/scholarships/disbursements`);
  }

  getPendingApplications(): Observable<ScholarshipApplication[]> {
    return this.http.get<ScholarshipApplication[]>(`${this.baseUrl}/scholarship-applications`);
  }

  getPendingApplicationsPage(p: {
    search?: string; page?: number; size?: number;
  }): Observable<Page<ScholarshipApplication>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search) params = params.set('search', p.search);
    return this.http.get<Page<ScholarshipApplication>>(`${this.baseUrl}/scholarship-applications`, { params });
  }

  approve(id: number, request: ScholarshipApprovalRequest): Observable<ScholarshipApplication> {
    return this.http.put<ScholarshipApplication>(`${this.baseUrl}/scholarship-applications/${id}/approve`, request);
  }

  reject(id: number, request: ScholarshipRejectionRequest): Observable<ScholarshipApplication> {
    return this.http.put<ScholarshipApplication>(`${this.baseUrl}/scholarship-applications/${id}/reject`, request);
  }

  renew(id: number): Observable<ScholarshipApplication> {
    return this.http.post<ScholarshipApplication>(`${this.baseUrl}/scholarship-applications/${id}/renew`, {});
  }

  disburse(id: number, request: DisbursementRequest): Observable<ScholarshipDisbursement> {
    return this.http.post<ScholarshipDisbursement>(`${this.baseUrl}/scholarship-applications/${id}/disburse`, request);
  }
}

