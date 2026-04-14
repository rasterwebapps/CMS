import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  FeeStructure, FeeStructureRequest, FeePayment, FeePaymentRequest,
  StudentFeeAllocation, StudentFeeAllocationRequest,
  CollectPaymentRequest, CollectPaymentResponse,
  PenaltyResponse, FeeExplorerResult, Receipt,
} from './finance.model';

@Injectable({
  providedIn: 'root',
})
export class FinanceService {
  private readonly http = inject(HttpClient);
  private readonly feeStructureUrl = `${environment.apiUrl}/fee-structures`;
  private readonly feePaymentUrl = `${environment.apiUrl}/fee-payments`;
  private readonly studentFeeUrl = `${environment.apiUrl}/student-fees`;

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

  finalizeStudentFee(request: StudentFeeAllocationRequest): Observable<StudentFeeAllocation> {
    return this.http.post<StudentFeeAllocation>(`${this.studentFeeUrl}/finalize`, request);
  }

  getSemesterBreakdown(studentId: number): Observable<StudentFeeAllocation> {
    return this.http.get<StudentFeeAllocation>(`${this.studentFeeUrl}/${studentId}/semester-breakdown`);
  }

  collectPayment(studentId: number, request: CollectPaymentRequest): Observable<CollectPaymentResponse> {
    return this.http.post<CollectPaymentResponse>(`${this.studentFeeUrl}/${studentId}/collect`, request);
  }

  getPenalties(studentId: number): Observable<PenaltyResponse> {
    return this.http.get<PenaltyResponse>(`${this.studentFeeUrl}/${studentId}/penalties`);
  }

  searchStudentFees(search?: string): Observable<FeeExplorerResult> {
    const params = search ? `?search=${encodeURIComponent(search)}` : '';
    return this.http.get<FeeExplorerResult>(`${this.studentFeeUrl}/explorer${params}`);
  }

  getReceipts(studentId: number): Observable<Receipt[]> {
    return this.http.get<Receipt[]>(`${this.studentFeeUrl}/${studentId}/receipts`);
  }

  getReceiptById(studentId: number, receiptId: number): Observable<Receipt> {
    return this.http.get<Receipt>(`${this.studentFeeUrl}/${studentId}/receipts/${receiptId}`);
  }
}
