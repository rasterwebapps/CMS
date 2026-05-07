import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  DisbursementRequest,
  ScholarshipApplication,
  ScholarshipApplicationRequest,
  ScholarshipApprovalRequest,
  ScholarshipDisbursement,
  ScholarshipEligibility,
  ScholarshipEligibilityRequest,
  ScholarshipRejectionRequest,
  ScholarshipType,
  ScholarshipTypeRequest,
} from './scholarship.model';

@Injectable({ providedIn: 'root' })
export class ScholarshipService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  getScholarshipTypes(): Observable<ScholarshipType[]> {
    return this.http.get<ScholarshipType[]>(`${this.baseUrl}/scholarships`);
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

  deactivateScholarshipType(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/scholarships/${id}`);
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

