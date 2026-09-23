import { Component, effect, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { PaymentModeLabelPipe } from '../../../shared/pipes/payment-mode-label.pipe';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { GuardianFeeService } from '../../guardian/guardian-fee.service';
import { StudentFeeAllocation, Receipt, PenaltyResponse } from '../../finance/finance.model';
import { WardContextService } from '../../../core/ward-context/ward-context.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../core/toast/toast.service';

declare const Razorpay: new (options: Record<string, unknown>) => { open(): void };

/**
 * Guardian self-service full fee ledger for the currently selected ward, mirroring
 * MyFeesComponent's shape, plus a "Pay Now" card that opens Razorpay's hosted Checkout.js for a
 * guardian-chosen (custom/partial allowed) amount. The order-creation call is ward-scoped and
 * re-validated server-side (GuardianFeeController); the checkout widget itself never touches this
 * app's backend with card/UPI details (hosted-checkout, PCI SAQ-A). Actual payment confirmation
 * is asynchronous (the backend webhook), so a successful Checkout.js handler callback only means
 * "submitted" — this component reloads receipts after a short delay rather than assuming the
 * ledger updated instantly.
 */
@Component({
  selector: 'app-ward-fees',
  standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule, InrPipe, PaymentModeLabelPipe, CmsStatusBadgeComponent],
  templateUrl: './ward-fees.component.html',
  styleUrl: './ward-fees.component.scss',
})
export class WardFeesComponent {
  private readonly guardianFeeService = inject(GuardianFeeService);
  protected readonly wardContext = inject(WardContextService);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly summary = signal<StudentFeeAllocation | null>(null);
  protected readonly summaryLoading = signal(false);
  protected readonly receipts = signal<Receipt[]>([]);
  protected readonly receiptsLoading = signal(false);
  protected readonly penalties = signal<PenaltyResponse | null>(null);
  protected readonly penaltiesLoading = signal(false);

  protected readonly paymentAmount = signal<number | null>(null);
  protected readonly payingNow = signal(false);

  protected readonly totalPending = () =>
    this.summary()?.installmentFees.reduce((sum, i) => sum + i.pendingAmount, 0) ?? 0;

  constructor() {
    effect(() => {
      const studentId = this.wardContext.selectedWardId();
      if (studentId == null) return;
      this.loadSummary(studentId);
      this.loadReceipts(studentId);
      this.loadPenalties(studentId);
    });
  }

  private loadSummary(studentId: number): void {
    this.summaryLoading.set(true);
    this.guardianFeeService.getWardFeeSummary(studentId).subscribe({
      next: (result) => { this.summary.set(result); this.summaryLoading.set(false); },
      error: () => { this.toast.error("Failed to load your ward's fee summary"); this.summaryLoading.set(false); },
    });
  }

  private loadReceipts(studentId: number): void {
    this.receiptsLoading.set(true);
    this.guardianFeeService.getWardReceipts(studentId).subscribe({
      next: (rows) => { this.receipts.set(rows); this.receiptsLoading.set(false); },
      error: () => { this.toast.error("Failed to load your ward's payment history"); this.receiptsLoading.set(false); },
    });
  }

  private loadPenalties(studentId: number): void {
    this.penaltiesLoading.set(true);
    this.guardianFeeService.getWardPenalties(studentId).subscribe({
      next: (result) => { this.penalties.set(result); this.penaltiesLoading.set(false); },
      error: () => { this.toast.error("Failed to load your ward's penalties"); this.penaltiesLoading.set(false); },
    });
  }

  protected payNow(): void {
    const studentId = this.wardContext.selectedWardId();
    const amount = this.paymentAmount();
    if (studentId == null || amount == null || amount <= 0) return;

    this.payingNow.set(true);
    this.guardianFeeService.createFeePaymentOrder(studentId, amount).subscribe({
      next: (order) => {
        this.payingNow.set(false);
        const ward = this.wardContext.selectedWard();
        const rzp = new Razorpay({
          key: order.razorpayKeyId,
          amount: Math.round(order.amount * 100),
          currency: order.currency,
          order_id: order.razorpayOrderId,
          name: 'OneCMS Fee Payment',
          description: ward ? `Fee payment for ${ward.fullName}` : 'Fee payment',
          prefill: { name: this.authService.username() },
          theme: { color: '#2563eb' },
          handler: () => {
            this.toast.success('Payment submitted — it will reflect in the ledger shortly');
            this.paymentAmount.set(null);
            setTimeout(() => this.loadReceipts(studentId), 3000);
          },
        });
        rzp.open();
      },
      error: (err) => {
        this.payingNow.set(false);
        this.toast.error(err?.error?.message ?? 'Failed to start payment');
      },
    });
  }
}
