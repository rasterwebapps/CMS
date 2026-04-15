import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
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

  deleteEnquiry(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  convertToStudent(enquiryId: number, studentId: number): Observable<Enquiry> {
    return this.http.put<Enquiry>(`${this.baseUrl}/${enquiryId}/convert?studentId=${studentId}`, {});
  }
}
