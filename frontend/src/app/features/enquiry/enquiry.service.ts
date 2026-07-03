import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  Enquiry,
  EnquiryRequest,
  FeeFinalizationRequest,
  FeeFinalizationResponse,
  EnquiryDocument,
  EnquiryDocumentRequest,
  EnquiryPaymentRequest,
  EnquiryPaymentResponse,
  EnquiryStatusHistoryResponse,
  EnquiryConversionRequest,
  EnquiryConversionPrefillResponse,
  EnquiryYearWiseFeeStatusResponse,
  DocumentVerificationStatus,
  EnquiryCreditApplication,
  Page,
} from './enquiry.model';

@Injectable({
  providedIn: 'root',
})
export class EnquiryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/enquiries`;

  getEnquiries(): Observable<Enquiry[]> {
    return this.http.get<Enquiry[]>(this.baseUrl);
  }

  getEnquiriesByDateRange(
    fromDate: string,
    toDate: string,
    status?: string,
  ): Observable<Enquiry[]> {
    let params = new HttpParams().set('fromDate', fromDate).set('toDate', toDate);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Enquiry[]>(this.baseUrl, { params });
  }

  getEnquiryById(id: number): Observable<Enquiry> {
    return this.http.get<Enquiry>(`${this.baseUrl}/${id}`);
  }

  getByStatus(status: string): Observable<Enquiry[]> {
    return this.http.get<Enquiry[]>(`${this.baseUrl}?status=${status}`);
  }

  getDocumentPending(): Observable<Enquiry[]> {
    return this.http.get<Enquiry[]>(`${this.baseUrl}/document-pending`);
  }

  getDocumentPendingPage(p: {
    search?: string; programId?: number | null; courseId?: number | null;
    studentType?: string | null; page?: number; size?: number;
  }): Observable<Page<Enquiry>> {
    let params = new HttpParams()
      .set('page', p.page ?? 0)
      .set('size', p.size ?? 25);
    if (p.search)      params = params.set('search', p.search);
    if (p.programId)   params = params.set('programId', p.programId);
    if (p.courseId)    params = params.set('courseId', p.courseId);
    if (p.studentType) params = params.set('studentType', p.studentType);
    return this.http.get<Page<Enquiry>>(`${this.baseUrl}/document-pending`, { params });
  }

  getAdmissionPending(): Observable<Enquiry[]> {
    return this.http.get<Enquiry[]>(`${this.baseUrl}/admission-pending`);
  }

  getAdmissionPendingPage(p: {
    search?: string; programId?: number | null; courseId?: number | null;
    studentType?: string | null; page?: number; size?: number;
  }): Observable<Page<Enquiry>> {
    let params = new HttpParams()
      .set('page', p.page ?? 0)
      .set('size', p.size ?? 25);
    if (p.search)      params = params.set('search', p.search);
    if (p.programId)   params = params.set('programId', p.programId);
    if (p.courseId)    params = params.set('courseId', p.courseId);
    if (p.studentType) params = params.set('studentType', p.studentType);
    return this.http.get<Page<Enquiry>>(`${this.baseUrl}/admission-pending`, { params });
  }

  createEnquiry(request: EnquiryRequest): Observable<Enquiry> {
    return this.http.post<Enquiry>(this.baseUrl, request);
  }

  updateEnquiry(id: number, request: EnquiryRequest): Observable<Enquiry> {
    return this.http.put<Enquiry>(`${this.baseUrl}/${id}`, request);
  }

  updateStatus(id: number, status: string): Observable<Enquiry> {
    return this.http.patch<Enquiry>(`${this.baseUrl}/${id}/status`, null, {
      params: new HttpParams().set('status', status),
    });
  }

  deleteEnquiry(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  convertToStudent(enquiryId: number, studentId: number): Observable<Enquiry> {
    return this.http.put<Enquiry>(
      `${this.baseUrl}/${enquiryId}/convert?studentId=${studentId}`,
      {},
    );
  }

  finalizeFees(
    enquiryId: number,
    request: FeeFinalizationRequest,
  ): Observable<FeeFinalizationResponse> {
    return this.http.post<FeeFinalizationResponse>(
      `${this.baseUrl}/${enquiryId}/finalize-fees`,
      request,
    );
  }

  getDocuments(enquiryId: number): Observable<EnquiryDocument[]> {
    return this.http.get<EnquiryDocument[]>(`${this.baseUrl}/${enquiryId}/documents`);
  }

  addDocument(enquiryId: number, request: EnquiryDocumentRequest): Observable<EnquiryDocument> {
    return this.http.post<EnquiryDocument>(`${this.baseUrl}/${enquiryId}/documents`, request);
  }

  updateDocument(
    enquiryId: number,
    documentId: number,
    request: EnquiryDocumentRequest,
  ): Observable<EnquiryDocument> {
    return this.http.put<EnquiryDocument>(
      `${this.baseUrl}/${enquiryId}/documents/${documentId}`,
      request,
    );
  }

  deleteDocument(enquiryId: number, documentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${enquiryId}/documents/${documentId}`);
  }

  /**
   * Uploads a scanned document file for the given enquiry. The backend
   * upserts the document by `documentType` — uploading the same type again
   * replaces the previously stored file.
   */
  uploadDocumentFile(
    enquiryId: number,
    documentType: string,
    file: File,
    remarks?: string,
    force = false,
  ): Observable<EnquiryDocument> {
    const form = new FormData();
    form.append('documentType', documentType);
    form.append('file', file, file.name);
    if (remarks) {
      form.append('remarks', remarks);
    }
    if (force) {
      form.append('force', 'true');
    }
    return this.http.post<EnquiryDocument>(`${this.baseUrl}/${enquiryId}/documents/upload`, form);
  }

  /**
   * Downloads the binary content of a stored document as a Blob, which the
   * caller can either trigger as a download or open in a new tab for viewing.
   */
  downloadDocumentFile(enquiryId: number, documentId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${enquiryId}/documents/${documentId}/download`, {
      responseType: 'blob',
    });
  }

  collectPayment(
    enquiryId: number,
    request: EnquiryPaymentRequest,
  ): Observable<EnquiryPaymentResponse> {
    return this.http.post<EnquiryPaymentResponse>(`${this.baseUrl}/${enquiryId}/payments`, request);
  }

  getPayments(enquiryId: number): Observable<EnquiryPaymentResponse[]> {
    return this.http.get<EnquiryPaymentResponse[]>(`${this.baseUrl}/${enquiryId}/payments`);
  }

  getStatusHistory(enquiryId: number): Observable<EnquiryStatusHistoryResponse[]> {
    return this.http.get<EnquiryStatusHistoryResponse[]>(
      `${this.baseUrl}/${enquiryId}/status-history`,
    );
  }

  submitDocuments(enquiryId: number): Observable<unknown> {
    return this.http.post(`${this.baseUrl}/${enquiryId}/submit-documents`, {});
  }

  getConversionPrefill(enquiryId: number): Observable<EnquiryConversionPrefillResponse> {
    return this.http.get<EnquiryConversionPrefillResponse>(
      `${this.baseUrl}/${enquiryId}/conversion-prefill`,
    );
  }

  convertEnquiry(enquiryId: number, request: EnquiryConversionRequest): Observable<Enquiry> {
    return this.http.post<Enquiry>(`${this.baseUrl}/${enquiryId}/convert`, request);
  }

  getYearWiseFeeStatus(enquiryId: number): Observable<EnquiryYearWiseFeeStatusResponse> {
    return this.http.get<EnquiryYearWiseFeeStatusResponse>(
      `${this.baseUrl}/${enquiryId}/year-wise-fee-status`,
    );
  }

  /**
   * Checks whether all mandatory documents for this enquiry have been
   * verified. Used as a pre-flight gate before completing admission.
   */
  getDocumentVerificationStatus(enquiryId: number): Observable<DocumentVerificationStatus> {
    return this.http.get<DocumentVerificationStatus>(
      `${this.baseUrl}/${enquiryId}/documents/verification-status`,
    );
  }

  getDocumentVerificationPending(): Observable<Enquiry[]> {
    return this.http.get<Enquiry[]>(`${this.baseUrl}/document-verification-pending`);
  }

  getDocumentVerificationPendingPage(p: {
    search?: string; programId?: number | null; courseId?: number | null;
    studentType?: string | null; page?: number; size?: number;
  }): Observable<Page<Enquiry>> {
    let params = new HttpParams()
      .set('page', p.page ?? 0)
      .set('size', p.size ?? 25);
    if (p.search)      params = params.set('search', p.search);
    if (p.programId)   params = params.set('programId', p.programId);
    if (p.courseId)    params = params.set('courseId', p.courseId);
    if (p.studentType) params = params.set('studentType', p.studentType);
    return this.http.get<Page<Enquiry>>(`${this.baseUrl}/document-verification-pending`, { params });
  }

  verifyDocument(enquiryId: number, documentId: number): Observable<EnquiryDocument> {
    return this.http.put<EnquiryDocument>(
      `${this.baseUrl}/${enquiryId}/documents/${documentId}/verify`,
      {},
    );
  }

  /**
   * Triggers the server-side auto-transition check for the given enquiry.
   * Used when there are no required document types to verify individually —
   * in that case verifyDocument is never called and the transition never fires,
   * so we call this explicitly to unblock the enquiry.
   */
  completeDocumentVerification(enquiryId: number): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl}/${enquiryId}/documents/complete-verification`,
      {},
    );
  }

  rejectDocument(enquiryId: number, documentId: number, rejectionComment: string): Observable<EnquiryDocument> {
    return this.http.put<EnquiryDocument>(
      `${this.baseUrl}/${enquiryId}/documents/${documentId}/reject`,
      { rejectionComment },
    );
  }

  getCreditApplications(enquiryId: number): Observable<EnquiryCreditApplication[]> {
    return this.http.get<EnquiryCreditApplication[]>(`${this.baseUrl}/${enquiryId}/credit-applications`);
  }

  getPage(p: {
    search?: string | null;
    fromDate?: string | null;
    toDate?: string | null;
    statuses?: string[];
    programId?: number | null;
    courseId?: number | null;
    studentType?: string | null;
    referralTypeName?: string | null;
    admissionQuota?: string | null;
    agentName?: string | null;
    admissionSource?: string | null;
    academicYearIds?: number[];
    page?: number;
    size?: number;
  }): Observable<Page<Enquiry>> {
    let params = new HttpParams()
      .set('page', p.page ?? 0)
      .set('size', p.size ?? 25);
    if (p.search)          params = params.set('search', p.search);
    if (p.fromDate)        params = params.set('fromDate', p.fromDate);
    if (p.toDate)          params = params.set('toDate', p.toDate);
    for (const s of (p.statuses ?? []))         params = params.append('statuses', s);
    if (p.programId)       params = params.set('programId', p.programId);
    if (p.courseId)        params = params.set('courseId', p.courseId);
    if (p.studentType)     params = params.set('studentType', p.studentType);
    if (p.referralTypeName) params = params.set('referralTypeName', p.referralTypeName);
    if (p.admissionQuota)  params = params.set('admissionQuota', p.admissionQuota);
    if (p.agentName)       params = params.set('agentName', p.agentName);
    if (p.admissionSource) params = params.set('admissionSource', p.admissionSource);
    for (const id of (p.academicYearIds ?? [])) params = params.append('academicYearIds', id);
    return this.http.get<Page<Enquiry>>(`${this.baseUrl}/page`, { params });
  }

  exportEnquiries(format: 'excel' | 'pdf', fromDate?: string, toDate?: string): Observable<Blob> {
    let params = new HttpParams().set('format', format);
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate)   params = params.set('toDate', toDate);
    return this.http.get(`${this.baseUrl}/export`, { params, responseType: 'blob' });
  }
}
