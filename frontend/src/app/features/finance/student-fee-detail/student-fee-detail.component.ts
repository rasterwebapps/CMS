import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { formatCurrency } from '@angular/common';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { FinanceService } from '../finance.service';
import { StudentFeeAllocation, InstallmentFeeDetail, Receipt } from '../finance.model';
import { CollectPaymentDialogComponent } from '../collect-payment-dialog/collect-payment-dialog.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';
import { PaymentModeLabelPipe } from '../../../shared/pipes/payment-mode-label.pipe';
import { printFeeReceipt, downloadFeeReceipt } from '../../../shared/utils/print-receipt.utils';

export interface ReceiptGroup {
  receiptNumber: string;
  paymentDate: string;
  paymentMode: string;
  transactionReference: string;
  totalAmount: number;
  lines: Receipt[];
}

@Component({
  selector: 'app-student-fee-detail',
  standalone: true,
  imports: [
    PaymentModeLabelPipe, InrPipe, RouterLink,
    MatDialogModule, MatTooltipModule, CmsStatusBadgeComponent,
  ],
  templateUrl: './student-fee-detail.component.html',
  styleUrl: './student-fee-detail.component.scss',
})
export class StudentFeeDetailComponent implements OnInit {
  private readonly route   = inject(ActivatedRoute);
  private readonly finance = inject(FinanceService);
  private readonly toast   = inject(ToastService);
  private readonly dialog  = inject(MatDialog);

  protected readonly loading      = signal(true);
  protected readonly initializing = signal(false);
  protected readonly initError    = signal(false);
  protected readonly allocation   = signal<StudentFeeAllocation | null>(null);
  protected readonly receiptGroups = signal<ReceiptGroup[]>([]);

  // ── Computed totals ──────────────────────────────────────────────────────────
  protected readonly totalFee = computed(() =>
    this.allocation()?.installmentFees.reduce((s, sf) => s + sf.amount, 0) ?? 0
  );
  protected readonly totalPaid = computed(() =>
    this.allocation()?.installmentFees.reduce((s, sf) => s + sf.amountPaid, 0) ?? 0
  );
  protected readonly totalOutstanding = computed(() =>
    this.allocation()?.installmentFees.reduce((s, sf) => s + sf.pendingAmount, 0) ?? 0
  );

  /** The first installment with a pending balance — next to receive payment. */
  protected readonly nextDueSemester = computed(() =>
    this.allocation()?.installmentFees.find(sf => sf.pendingAmount > 0) ?? null
  );

  private studentId!: number;

  ngOnInit(): void {
    this.studentId = Number(this.route.snapshot.paramMap.get('studentId'));
    this.loadAll();
  }

  protected isOverdue(sem: InstallmentFeeDetail): boolean {
    return sem.pendingAmount > 0 && new Date(sem.dueDate) < new Date();
  }

  protected isNextDue(sem: InstallmentFeeDetail): boolean {
    return this.nextDueSemester()?.id === sem.id;
  }

  // ── Payment dialog ────────────────────────────────────────────────────────────
  protected openCollectPaymentDialog(): void {
    const ref = this.dialog.open(CollectPaymentDialogComponent, {
      width: '520px',
      data: { studentId: this.studentId },
    });
    ref.afterClosed().subscribe((result) => {
      if (result) {
        const breakdown = result.installmentBreakdown
          ?.map((s: any) => `${s.installmentLabel}: ${formatCurrency(s.amountApplied, 'en-IN', '₹', 'INR', '1.0-0')}`)
          .join(', ') ?? result.allocationSummary;
        this.toast.success(`Receipt ${result.receiptNumber} — ${breakdown}`);
        this.loadAll();
      }
    });
  }

  protected retryInit(): void {
    this.initError.set(false);
    this.autoInitializeAllocation();
  }

  // ── Data loading ──────────────────────────────────────────────────────────────
  private loadAll(): void {
    this.loading.set(true);
    this.initError.set(false);

    this.finance.getFeeAllocationStatus(this.studentId).subscribe({
      next: (data) => {
        this.allocation.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 404) {
          this.autoInitializeAllocation();
        } else {
          this.toast.error('Failed to load fee details');
        }
      },
    });

    this.finance.getReceipts(this.studentId).subscribe({
      next: (data) => this.receiptGroups.set(this.groupReceipts(data)),
    });
  }

  private autoInitializeAllocation(): void {
    this.initializing.set(true);

    this.finance.getEnquiryYearFees(this.studentId).subscribe({
      next: (fees) => {
        if (!fees.length) {
          this.initializing.set(false);
          this.initError.set(true);
          return;
        }
        const yearFees = fees.map(f => ({ yearNumber: f.yearNumber, amount: f.amount }));
        const totalFee = yearFees.reduce((s, f) => s + f.amount, 0);

        this.finance.createStudentFeeAllocation({ studentId: this.studentId, totalFee, yearFees }).subscribe({
          next: (data) => {
            this.allocation.set(data);
            this.initializing.set(false);
            this.toast.success('Term-wise installments created from finalized fee');
          },
          error: () => {
            this.initializing.set(false);
            this.initError.set(true);
          },
        });
      },
      error: () => {
        this.initializing.set(false);
        this.initError.set(true);
      },
    });
  }

  // ── Receipt actions ───────────────────────────────────────────────────────────

  protected viewReceipt(group: ReceiptGroup): void {
    void printFeeReceipt(this.toReceiptPrintData(group));
  }

  protected downloadReceipt(group: ReceiptGroup): void {
    void downloadFeeReceipt(this.toReceiptPrintData(group));
  }

  private toReceiptPrintData(group: ReceiptGroup) {
    const alloc = this.allocation();
    return {
      receiptNumber:        group.receiptNumber,
      payerName:            alloc?.studentName ?? group.lines[0]?.studentName ?? '',
      payerIdentifier:      alloc?.rollNumber  ?? group.lines[0]?.rollNumber  ?? '',
      programName:          alloc?.programName ?? '',
      amountPaid:           group.totalAmount,
      paymentDate:          group.paymentDate,
      paymentMode:          group.paymentMode,
      transactionReference: group.transactionReference || null,
      feeCategory:          null as null,
      installmentBreakdown: group.lines.map(l => ({
        installmentLabel: l.installmentLabel ?? '',
        amountApplied:    l.amountPaid,
      })),
    };
  }

  private groupReceipts(receipts: Receipt[]): ReceiptGroup[] {
    const map = new Map<string, ReceiptGroup>();
    for (const r of receipts) {
      const existing = map.get(r.receiptNumber);
      if (existing) {
        existing.lines.push(r);
        existing.totalAmount += r.amountPaid;
      } else {
        map.set(r.receiptNumber, {
          receiptNumber: r.receiptNumber,
          paymentDate: r.paymentDate,
          paymentMode: r.paymentMode,
          transactionReference: r.transactionReference,
          totalAmount: r.amountPaid,
          lines: [r],
        });
      }
    }
    return Array.from(map.values());
  }
}
