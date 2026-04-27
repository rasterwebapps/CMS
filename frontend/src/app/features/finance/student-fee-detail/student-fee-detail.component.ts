import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DecimalPipe } from '@angular/common';
import { FinanceService } from '../finance.service';
import {
  StudentFeeAllocation, SemesterFeeDetail, Receipt,
  EnquiryYearFee, CreateAllocationYearFee,
} from '../finance.model';
import { CollectPaymentDialogComponent } from '../collect-payment-dialog/collect-payment-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';

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
    DecimalPipe, RouterLink, ReactiveFormsModule,
    MatIconModule, MatProgressSpinnerModule,
    MatDialogModule, MatTooltipModule,
    PageHeaderComponent, CmsStatusBadgeComponent,
  ],
  templateUrl: './student-fee-detail.component.html',
  styleUrl: './student-fee-detail.component.scss',
})
export class StudentFeeDetailComponent implements OnInit {
  private readonly route   = inject(ActivatedRoute);
  private readonly finance = inject(FinanceService);
  private readonly toast   = inject(ToastService);
  private readonly dialog  = inject(MatDialog);
  private readonly fb      = inject(FormBuilder);

  protected readonly loading         = signal(true);
  protected readonly noAllocation    = signal(false);
  protected readonly setupLoading    = signal(false);
  protected readonly savingAlloc     = signal(false);
  protected readonly allocation      = signal<StudentFeeAllocation | null>(null);
  protected readonly receiptGroups   = signal<ReceiptGroup[]>([]);
  protected readonly enquiryYearFees = signal<EnquiryYearFee[]>([]);
  protected readonly yearFeeRows     = signal<{ yearNumber: number; amount: number; dueDate: string }[]>([]);

  protected readonly setupForm: FormGroup = this.fb.group({
    discountAmount: [0, [Validators.min(0)]],
    discountReason: [''],
  });

  // ── Computed totals ──────────────────────────────────────────────────────────
  protected readonly totalFee = computed(() =>
    this.allocation()?.semesterFees.reduce((s, sf) => s + sf.amount, 0) ?? 0
  );
  protected readonly totalPaid = computed(() =>
    this.allocation()?.semesterFees.reduce((s, sf) => s + sf.amountPaid, 0) ?? 0
  );
  protected readonly totalOutstanding = computed(() =>
    this.allocation()?.semesterFees.reduce((s, sf) => s + sf.pendingAmount, 0) ?? 0
  );

  /** The first semester with a pending balance — next to receive payment. */
  protected readonly nextDueSemester = computed(() =>
    this.allocation()?.semesterFees.find(sf => sf.pendingAmount > 0) ?? null
  );

  protected readonly setupTotal = computed(() =>
    this.yearFeeRows().reduce((s, r) => s + r.amount, 0)
  );
  protected readonly setupNetFee = computed(() => {
    const discount = Number(this.setupForm.get('discountAmount')?.value) || 0;
    return Math.max(0, this.setupTotal() - discount);
  });

  private studentId!: number;

  ngOnInit(): void {
    this.studentId = Number(this.route.snapshot.paramMap.get('studentId'));
    this.loadAll();
  }

  protected isOverdue(sem: SemesterFeeDetail): boolean {
    return sem.pendingAmount > 0 && new Date(sem.dueDate) < new Date();
  }

  protected isNextDue(sem: SemesterFeeDetail): boolean {
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
        const breakdown = result.semesterBreakdown
          ?.map((s: any) => `${s.semesterLabel}: ₹${s.amountApplied.toLocaleString('en-IN')}`)
          .join(', ') ?? result.allocationSummary;
        this.toast.success(`Receipt ${result.receiptNumber} — ${breakdown}`);
        this.loadAll();
      }
    });
  }

  // ── Setup: year fee row editing ───────────────────────────────────────────────
  protected updateYearAmount(index: number, value: string): void {
    const rows = [...this.yearFeeRows()];
    rows[index] = { ...rows[index], amount: Math.max(0, parseFloat(value) || 0) };
    this.yearFeeRows.set(rows);
  }

  protected updateYearDueDate(index: number, value: string): void {
    const rows = [...this.yearFeeRows()];
    rows[index] = { ...rows[index], dueDate: value };
    this.yearFeeRows.set(rows);
  }

  // ── Setup: create allocation ──────────────────────────────────────────────────
  protected createAllocation(): void {
    if (this.yearFeeRows().some(r => !r.dueDate)) {
      this.toast.error('Please set a due date for all years');
      return;
    }
    const discount = Number(this.setupForm.get('discountAmount')?.value) || 0;
    const reason   = this.setupForm.get('discountReason')?.value?.trim() || undefined;

    const yearFees: CreateAllocationYearFee[] = this.yearFeeRows().map(r => ({
      yearNumber: r.yearNumber,
      amount: r.amount,
      dueDate: r.dueDate,
    }));

    const request = {
      studentId: this.studentId,
      totalFee: this.setupTotal(),
      discountAmount: discount || undefined,
      discountReason: reason,
      agentCommission: undefined,
      yearFees,
    };

    this.savingAlloc.set(true);
    this.finance.createStudentFeeAllocation(request).subscribe({
      next: (data) => {
        this.allocation.set(data);
        this.noAllocation.set(false);
        this.savingAlloc.set(false);
        this.toast.success('Fee allocation created — semesters generated automatically');
        this.loadAll();
      },
      error: () => {
        this.toast.error('Failed to create fee allocation');
        this.savingAlloc.set(false);
      },
    });
  }

  // ── Data loading ──────────────────────────────────────────────────────────────
  private loadAll(): void {
    this.loading.set(true);
    this.noAllocation.set(false);

    this.finance.getSemesterStatus(this.studentId).subscribe({
      next: (data) => {
        this.allocation.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        if (err.status === 404) {
          // No allocation yet — load enquiry year fees to pre-fill the setup form
          this.noAllocation.set(true);
          this.loadEnquiryYearFees();
        } else {
          this.toast.error('Failed to load fee details');
        }
        this.loading.set(false);
      },
    });

    this.finance.getReceipts(this.studentId).subscribe({
      next: (data) => this.receiptGroups.set(this.groupReceipts(data)),
    });
  }

  private loadEnquiryYearFees(): void {
    this.setupLoading.set(true);
    this.finance.getEnquiryYearFees(this.studentId).subscribe({
      next: (fees) => {
        this.enquiryYearFees.set(fees);
        this.yearFeeRows.set(fees.map(f => ({
          yearNumber: f.yearNumber,
          amount: f.amount,
          dueDate: f.suggestedDueDate,
        })));
        this.setupLoading.set(false);
      },
      error: () => {
        // No linked enquiry — show empty setup form with default rows
        this.yearFeeRows.set([
          { yearNumber: 1, amount: 0, dueDate: '' },
          { yearNumber: 2, amount: 0, dueDate: '' },
        ]);
        this.setupLoading.set(false);
      },
    });
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
