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
  ): Observable<EnquiryDocument> {
    const form = new FormData();
    form.append('documentType', documentType);
    form.append('file', file, file.name);
    if (remarks) {
      form.append('remarks', remarks);
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
}
