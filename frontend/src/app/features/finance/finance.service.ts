import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import {
  FeeState, FeeStructure, FeeStructureRequest,
  StudentFeeAllocation, StudentFeeAllocationRequest,
  CollectPaymentRequest, CollectPaymentResponse,
  PenaltyResponse, FeeExplorerResult, FeeExplorerParams, Page, StudentFeeSummary,
  Receipt, ReceiptSummary, BulkFeeStructureRequest,
  GroupedFeeStructure, EnquiryYearFee, CreateAllocationRequest, UnifiedReceiptSummary,
  FeeRefundRequest, FeeRefundResponse, FeeRefundSummary,
  FeeRefundApprovalRequest, FeeRefundRejectionRequest, EnquiryCreditApplication,
} from './finance.model';

@Injectable({
  providedIn: 'root',
})
export class FinanceService {
  private readonly http = inject(HttpClient);
  private readonly feeStructureUrl = `${environment.apiUrl}/fee-structures`;

  private readonly studentFeeUrl = `${environment.apiUrl}/student-fees`;

  getFeeStates(): Observable<FeeState[]> {
    return this.http.get<FeeState[]>(`${environment.apiUrl}/fee-states`);
  }

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

  deleteGroupedFeeStructures(
    programId: number, academicYearId: number, courseId: number | undefined,
    quota: string, feeStateId: number, gender: string
  ): Observable<void> {
    let httpParams = new HttpParams()
      .set('programId', programId.toString())
      .set('academicYearId', academicYearId.toString())
      .set('quota', quota)
      .set('feeStateId', feeStateId.toString())
      .set('gender', gender);
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

  finalizeStudentFee(request: StudentFeeAllocationRequest): Observable<StudentFeeAllocation> {
    return this.http.post<StudentFeeAllocation>(`${this.studentFeeUrl}/finalize`, request);
  }

  getInstallmentBreakdown(studentId: number): Observable<StudentFeeAllocation> {
    return this.http.get<StudentFeeAllocation>(`${this.studentFeeUrl}/${studentId}/semester-breakdown`);
  }

  getFeeAllocationStatus(studentId: number): Observable<StudentFeeAllocation> {
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

  collectAdvancePayment(studentId: number, request: CollectPaymentRequest): Observable<CollectPaymentResponse> {
    return this.http.post<CollectPaymentResponse>(`${this.studentFeeUrl}/${studentId}/collect-advance`, request);
  }

  getPenalties(studentId: number): Observable<PenaltyResponse> {
    return this.http.get<PenaltyResponse>(`${this.studentFeeUrl}/${studentId}/penalties`);
  }

  searchStudentFees(search?: string): Observable<FeeExplorerResult> {
    const params = search ? `?search=${encodeURIComponent(search)}&legacy=true` : '?legacy=true';
    return this.http.get<FeeExplorerResult>(`${this.studentFeeUrl}/explorer${params}`);
  }

  searchStudentFeesPage(p: FeeExplorerParams): Observable<Page<StudentFeeSummary>> {
    let params = new HttpParams()
      .set('page', p.page ?? 0)
      .set('size', p.size ?? 25);
    if (p.sort)   params = params.set('sort', p.sort);
    if (p.search && p.search.length >= 2) params = params.set('search', p.search);
    return this.http.get<Page<StudentFeeSummary>>(`${this.studentFeeUrl}/explorer`, { params });
  }

  getReceipts(studentId: number): Observable<Receipt[]> {
    return this.http.get<Receipt[]>(`${this.studentFeeUrl}/${studentId}/receipts`);
  }

  getReceiptById(studentId: number, receiptId: number): Observable<Receipt> {
    return this.http.get<Receipt>(`${this.studentFeeUrl}/${studentId}/receipts/${receiptId}`);
  }

  getAllReceiptSummaries(): Observable<ReceiptSummary[]> {
    return this.http.get<ReceiptSummary[]>(`${this.studentFeeUrl}/receipts`);
  }

  /** Returns all receipts (student + enquiry) merged. Backed by GET /api/v1/receipts. */
  getUnifiedReceipts(): Observable<UnifiedReceiptSummary[]> {
    return this.http.get<UnifiedReceiptSummary[]>(`${environment.apiUrl}/receipts`);
  }

  /** Returns a single receipt by its receipt number. Backed by GET /api/v1/receipts/{receiptNumber}. */
  getReceiptByNumber(receiptNumber: string): Observable<UnifiedReceiptSummary> {
    return this.http.get<UnifiedReceiptSummary>(`${environment.apiUrl}/receipts/${encodeURIComponent(receiptNumber)}`);
  }

  /** Unified refund initiation — works for STUDENT and ENQUIRY receipts. */
  createRefund(request: FeeRefundRequest): Observable<FeeRefundResponse> {
    return this.http.post<FeeRefundResponse>(`${this.studentFeeUrl}/refunds`, request);
  }

  /** Returns all refunds — PENDING first (403 if caller lacks FEE_REFUND_APPROVE). */
  getAllRefunds(): Observable<FeeRefundSummary[]> {
    return this.http.get<FeeRefundSummary[]>(`${this.studentFeeUrl}/refunds`);
  }

  /** Returns all PENDING refund requests (403 if caller lacks FEE_REFUND_APPROVE). */
  getPendingRefunds(): Observable<FeeRefundSummary[]> {
    return this.http.get<FeeRefundSummary[]>(`${this.studentFeeUrl}/refunds/pending`);
  }

  /** Step 2a — approve a pending refund and record payment details. */
  approveRefund(refundId: number, request: FeeRefundApprovalRequest): Observable<FeeRefundSummary> {
    return this.http.post<FeeRefundSummary>(
      `${this.studentFeeUrl}/refunds/${refundId}/approve`, request);
  }

  /** Step 2b — reject a pending refund request. */
  rejectRefund(refundId: number, request: FeeRefundRejectionRequest): Observable<FeeRefundSummary> {
    return this.http.post<FeeRefundSummary>(
      `${this.studentFeeUrl}/refunds/${refundId}/reject`, request);
  }

  /** Push a PENDING (or PAYMENT_FAILED) refund to OneBook for payment. */
  approveRefundViaOneBook(refundId: number): Observable<{ referenceId: string; status: string }> {
    return this.http.post<{ referenceId: string; status: string }>(
      `${this.studentFeeUrl}/refunds/${refundId}/approve-onebook`, {});
  }

  /** Returns all pre-enrollment credit applications for a converted student. */
  getCreditApplications(studentId: number): Observable<EnquiryCreditApplication[]> {
    return this.http.get<EnquiryCreditApplication[]>(`${this.studentFeeUrl}/${studentId}/credit-applications`);
  }
}
