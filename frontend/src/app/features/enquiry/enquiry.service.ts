import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { Enquiry, EnquiryRequest } from './enquiry.model';

@Injectable({
  providedIn: 'root',
})
export class EnquiryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/enquiries`;

  getEnquiries(): Observable<Enquiry[]> {
    return this.http.get<Enquiry[]>(this.baseUrl);
  }

  getEnquiriesByDateRange(fromDate: string, toDate: string, status?: string): Observable<Enquiry[]> {
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
    return this.http.put<Enquiry>(`${this.baseUrl}/${enquiryId}/convert?studentId=${studentId}`, {});
  }
}
