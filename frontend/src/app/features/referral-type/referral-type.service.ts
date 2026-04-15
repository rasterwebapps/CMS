import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { ReferralType, ReferralTypeRequest } from './referral-type.model';

@Injectable({
  providedIn: 'root',
})
export class ReferralTypeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/referral-types`;

  getReferralTypes(): Observable<ReferralType[]> {
    return this.http.get<ReferralType[]>(this.baseUrl);
  }

  getActiveReferralTypes(): Observable<ReferralType[]> {
    return this.http.get<ReferralType[]>(`${this.baseUrl}?activeOnly=true`);
  }

  getReferralTypeById(id: number): Observable<ReferralType> {
    return this.http.get<ReferralType>(`${this.baseUrl}/${id}`);
  }

  createReferralType(request: ReferralTypeRequest): Observable<ReferralType> {
    return this.http.post<ReferralType>(this.baseUrl, request);
  }

  updateReferralType(id: number, request: ReferralTypeRequest): Observable<ReferralType> {
    return this.http.put<ReferralType>(`${this.baseUrl}/${id}`, request);
  }

  deleteReferralType(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
