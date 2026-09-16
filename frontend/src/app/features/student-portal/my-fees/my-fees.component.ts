import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { PaymentModeLabelPipe } from '../../../shared/pipes/payment-mode-label.pipe';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { FinanceService } from '../../finance/finance.service';
import { StudentFeeAllocation, Receipt, PenaltyResponse } from '../../finance/finance.model';
import { ToastService } from '../../../core/toast/toast.service';

/**
 * Student self-service full fee ledger — semester breakdown, payment history (receipts), and
 * outstanding penalties, all for the caller's own linked Student record only. Never accepts a
 * studentId; the backend resolves it from the JWT (see StudentFeeController's /my/* endpoints,
 * StudentFeeSelfServiceService), so this component cannot be pointed at anyone else's data even
 * by a modified request — same self-service shape as StudentDashboardComponent's Attendance/Exam
 * Results sections.
 */
@Component({
  selector: 'app-my-fees',
  standalone: true,
  imports: [DatePipe, InrPipe, PaymentModeLabelPipe, CmsStatusBadgeComponent],
  templateUrl: './my-fees.component.html',
  styleUrl: './my-fees.component.scss',
})
export class MyFeesComponent implements OnInit {
  private readonly financeService = inject(FinanceService);
  private readonly toast = inject(ToastService);

  protected readonly summary = signal<StudentFeeAllocation | null>(null);
  protected readonly summaryLoading = signal(false);
  protected readonly receipts = signal<Receipt[]>([]);
  protected readonly receiptsLoading = signal(false);
  protected readonly penalties = signal<PenaltyResponse | null>(null);
  protected readonly penaltiesLoading = signal(false);

  ngOnInit(): void {
    this.loadSummary();
    this.loadReceipts();
    this.loadPenalties();
  }

  private loadSummary(): void {
    this.summaryLoading.set(true);
    this.financeService.getMyFeeSummary().subscribe({
      next: (result) => { this.summary.set(result); this.summaryLoading.set(false); },
      error: () => { this.toast.error('Failed to load your fee summary'); this.summaryLoading.set(false); },
    });
  }

  private loadReceipts(): void {
    this.receiptsLoading.set(true);
    this.financeService.getMyReceipts().subscribe({
      next: (rows) => { this.receipts.set(rows); this.receiptsLoading.set(false); },
      error: () => { this.toast.error('Failed to load your payment history'); this.receiptsLoading.set(false); },
    });
  }

  private loadPenalties(): void {
    this.penaltiesLoading.set(true);
    this.financeService.getMyPenalties().subscribe({
      next: (result) => { this.penalties.set(result); this.penaltiesLoading.set(false); },
      error: () => { this.toast.error('Failed to load your penalties'); this.penaltiesLoading.set(false); },
    });
  }
}
