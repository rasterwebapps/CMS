import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FeeStructure, FeeStructureRequest, FeePayment, FeePaymentRequest } from './finance.model';

@Injectable({
  providedIn: 'root',
})
export class FinanceService {
  private readonly http = inject(HttpClient);
  private readonly feeStructureUrl = `${environment.apiUrl}/fee-structures`;
  private readonly feePaymentUrl = `${environment.apiUrl}/fee-payments`;

  getFeeStructures(): Observable<FeeStructure[]> {
    return this.http.get<FeeStructure[]>(this.feeStructureUrl);
  }

  getFeeStructureById(id: number): Observable<FeeStructure> {
    return this.http.get<FeeStructure>(`${this.feeStructureUrl}/${id}`);
  }

  createFeeStructure(request: FeeStructureRequest): Observable<FeeStructure> {
    return this.http.post<FeeStructure>(this.feeStructureUrl, request);
  }

  updateFeeStructure(id: number, request: FeeStructureRequest): Observable<FeeStructure> {
    return this.http.put<FeeStructure>(`${this.feeStructureUrl}/${id}`, request);
  }

  deleteFeeStructure(id: number): Observable<void> {
    return this.http.delete<void>(`${this.feeStructureUrl}/${id}`);
  }

  getPayments(): Observable<FeePayment[]> {
    return this.http.get<FeePayment[]>(this.feePaymentUrl);
  }

  getPaymentsByStudent(studentId: number): Observable<FeePayment[]> {
    return this.http.get<FeePayment[]>(`${this.feePaymentUrl}?studentId=${studentId}`);
  }

  createPayment(request: FeePaymentRequest): Observable<FeePayment> {
    return this.http.post<FeePayment>(this.feePaymentUrl, request);
  }

  deletePayment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.feePaymentUrl}/${id}`);
  }
}
