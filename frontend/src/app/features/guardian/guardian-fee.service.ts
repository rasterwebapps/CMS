import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments';
import { StudentFeeAllocation, Receipt, PenaltyResponse } from '../finance/finance.model';
import { RazorpayOrderResponse } from './guardian-fee.model';

/** Ward fee self-service for guardians -- every call is scoped to one ward (studentId), which the
 *  backend re-validates as actually belonging to the caller before returning anything
 *  (GuardianFeeController#assertIsMyWard). Mirrors FinanceService's my/* self-service shape. */
@Injectable({
  providedIn: 'root',
})
export class GuardianFeeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/guardian/wards`;

  getWardFeeSummary(studentId: number): Observable<StudentFeeAllocation | null> {
    return this.http.get<StudentFeeAllocation | null>(`${this.baseUrl}/${studentId}/fee-summary`);
  }

  getWardReceipts(studentId: number): Observable<Receipt[]> {
    return this.http.get<Receipt[]>(`${this.baseUrl}/${studentId}/fee-receipts`);
  }

  getWardPenalties(studentId: number): Observable<PenaltyResponse | null> {
    return this.http.get<PenaltyResponse | null>(`${this.baseUrl}/${studentId}/fee-penalties`);
  }

  /** Creates a Razorpay order for a guardian-chosen amount (custom/partial allowed) -- the
   *  returned razorpayOrderId/razorpayKeyId are then handed to Razorpay's hosted Checkout.js.
   *  Payment is only actually confirmed asynchronously by the backend webhook, never by this
   *  call's response alone. */
  createFeePaymentOrder(studentId: number, amount: number): Observable<RazorpayOrderResponse> {
    return this.http.post<RazorpayOrderResponse>(`${this.baseUrl}/${studentId}/fee-payments/orders`, { amount });
  }
}
