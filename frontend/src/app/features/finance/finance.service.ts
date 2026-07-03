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

  exportFeeExplorer(
    format: 'excel' | 'pdf',
    filters: {
      search?: string | null;
      program?: string | null;
      academicYear?: string | null;
      yearOfStudy?: number | null;
      allocationStatus?: string | null;
    } = {},
  ): Observable<Blob> {
    let params = new HttpParams().set('format', format);
    if (filters.search && filters.search.length >= 2) params = params.set('search', filters.search);
    if (filters.program && filters.program !== 'ALL')         params = params.set('program', filters.program);
    if (filters.academicYear && filters.academicYear !== 'ALL') params = params.set('academicYear', filters.academicYear);
    if (filters.yearOfStudy != null)                           params = params.set('yearOfStudy', filters.yearOfStudy);
    if (filters.allocationStatus && filters.allocationStatus !== 'ALL') params = params.set('allocationStatus', filters.allocationStatus);
    return this.http.get(`${this.studentFeeUrl}/explorer/export`, { params, responseType: 'blob' });
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

  getUnifiedReceiptsPage(p: {
    search?: string; paymentMode?: string; payerType?: string;
    fromDate?: string; toDate?: string; page?: number; size?: number;
  }): Observable<Page<UnifiedReceiptSummary>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search)      params = params.set('search', p.search);
    if (p.paymentMode) params = params.set('paymentMode', p.paymentMode);
    if (p.payerType)   params = params.set('payerType', p.payerType);
    if (p.fromDate)    params = params.set('fromDate', p.fromDate);
    if (p.toDate)      params = params.set('toDate', p.toDate);
    return this.http.get<Page<UnifiedReceiptSummary>>(`${environment.apiUrl}/receipts`, { params });
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

  getFeeRefundsPage(p: {
    search?: string; status?: string; entityType?: string;
    fromDate?: string; toDate?: string; page?: number; size?: number;
  }): Observable<Page<FeeRefundSummary>> {
    let params = new HttpParams().set('page', p.page ?? 0).set('size', p.size ?? 25);
    if (p.search)      params = params.set('search', p.search);
    if (p.status)      params = params.set('status', p.status);
    if (p.entityType)  params = params.set('entityType', p.entityType);
    if (p.fromDate)    params = params.set('fromDate', p.fromDate);
    if (p.toDate)      params = params.set('toDate', p.toDate);
    return this.http.get<Page<FeeRefundSummary>>(`${this.studentFeeUrl}/refunds`, { params });
  }

  /** Returns all PENDING refund requests (403 if caller lacks FEE_REFUND_APPROVE). */
  getPendingRefunds(): Observable<FeeRefundSummary[]> {
    return this.http.get<FeeRefundSummary[]>(`${this.studentFeeUrl}/refunds/pending`);
  }

  exportReceipts(
    format: 'excel' | 'pdf',
    filters: { search?: string | null; paymentMode?: string | null; payerType?: string | null; fromDate?: string | null; toDate?: string | null } = {},
  ): Observable<Blob> {
    let params = new HttpParams().set('format', format);
    if (filters.search)      params = params.set('search', filters.search);
    if (filters.paymentMode) params = params.set('paymentMode', filters.paymentMode);
    if (filters.payerType)   params = params.set('payerType', filters.payerType);
    if (filters.fromDate)    params = params.set('fromDate', filters.fromDate);
    if (filters.toDate)      params = params.set('toDate', filters.toDate);
    return this.http.get(`${environment.apiUrl}/receipts/export`, { params, responseType: 'blob' });
  }

  exportRefunds(
    format: 'excel' | 'pdf',
    filters: { search?: string | null; status?: string | null; entityType?: string | null; fromDate?: string | null; toDate?: string | null } = {},
  ): Observable<Blob> {
    let params = new HttpParams().set('format', format);
    if (filters.search)      params = params.set('search', filters.search);
    if (filters.status)      params = params.set('status', filters.status);
    if (filters.entityType)  params = params.set('entityType', filters.entityType);
    if (filters.fromDate)    params = params.set('fromDate', filters.fromDate);
    if (filters.toDate)      params = params.set('toDate', filters.toDate);
    return this.http.get(`${this.studentFeeUrl}/refunds/export`, { params, responseType: 'blob' });
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
