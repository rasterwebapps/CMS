import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  FeeStructure, FeeStructureRequest, FeePayment, FeePaymentRequest,
  StudentFeeAllocation, StudentFeeAllocationRequest,
  CollectPaymentRequest, CollectPaymentResponse,
  PenaltyResponse, FeeExplorerResult, Receipt, BulkFeeStructureRequest,
  GroupedFeeStructure, EnquiryYearFee, CreateAllocationRequest,
} from './finance.model';

@Injectable({
  providedIn: 'root',
})
export class FinanceService {
  private readonly http = inject(HttpClient);
  private readonly feeStructureUrl = `${environment.apiUrl}/fee-structures`;
  private readonly feePaymentUrl = `${environment.apiUrl}/fee-payments`;
  private readonly studentFeeUrl = `${environment.apiUrl}/student-fees`;

  bulkCreateFeeStructures(request: BulkFeeStructureRequest): Observable<FeeStructure[]> {
    return this.http.post<FeeStructure[]>(`${this.feeStructureUrl}/bulk`, request);
  }

  bulkUpdateFeeStructures(request: BulkFeeStructureRequest): Observable<FeeStructure[]> {
    return this.http.put<FeeStructure[]>(`${this.feeStructureUrl}/bulk`, request);
  }

  getGroupedFeeStructures(params?: {
    programId?: number;
    academicYearId?: number;
    courseId?: number;
  }): Observable<GroupedFeeStructure[]> {
    let httpParams = new HttpParams();
    if (params?.programId) httpParams = httpParams.set('programId', params.programId.toString());
    if (params?.academicYearId) httpParams = httpParams.set('academicYearId', params.academicYearId.toString());
    if (params?.courseId) httpParams = httpParams.set('courseId', params.courseId.toString());
    return this.http.get<GroupedFeeStructure[]>(`${this.feeStructureUrl}/grouped`, { params: httpParams });
  }

  deleteGroupedFeeStructures(programId: number, academicYearId: number, courseId?: number): Observable<void> {
    let httpParams = new HttpParams()
      .set('programId', programId.toString())
      .set('academicYearId', academicYearId.toString());
    if (courseId) httpParams = httpParams.set('courseId', courseId.toString());
    return this.http.delete<void>(`${this.feeStructureUrl}/group`, { params: httpParams });
  }

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

  getFeeStructuresByProgramAndCourse(programId: number, courseId: number): Observable<FeeStructure[]> {
    return this.http.get<FeeStructure[]>(`${this.feeStructureUrl}?programId=${programId}&courseId=${courseId}`);
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

  getSemesterStatus(studentId: number): Observable<StudentFeeAllocation> {
    return this.http.get<StudentFeeAllocation>(`${this.studentFeeUrl}/${studentId}/semester-status`);
  }

  allocationExists(studentId: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.studentFeeUrl}/${studentId}/allocation-exists`);
  }

  getEnquiryYearFees(studentId: number): Observable<EnquiryYearFee[]> {
    return this.http.get<EnquiryYearFee[]>(`${this.studentFeeUrl}/${studentId}/enquiry-year-fees`);
  }

  createStudentFeeAllocation(request: CreateAllocationRequest): Observable<StudentFeeAllocation> {
    return this.http.post<StudentFeeAllocation>(`${this.studentFeeUrl}/finalize`, request);
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
