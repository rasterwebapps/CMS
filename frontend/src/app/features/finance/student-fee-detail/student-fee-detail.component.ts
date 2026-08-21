import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { PaymentModeLabelPipe } from '../../../shared/pipes/payment-mode-label.pipe';
import { FinanceService } from '../finance.service';
import { StudentFeeAllocation, InstallmentFeeDetail, Receipt, EnquiryCreditApplication, ReceiptDisplayData, CollectPaymentRequest } from '../finance.model';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CashDenominationComponent } from '../../../shared/cash-denomination/cash-denomination.component';
import { FeeReceiptDialogComponent } from '../../../shared/fee-receipt-dialog/fee-receipt-dialog.component';
import { ToastService } from '../../../core/toast/toast.service';
import { printFeeReceipt, downloadFeeReceipt } from '../../../shared/utils/print-receipt.utils';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { STUDENT_FEE_DETAIL_TOUR, STUDENT_FEE_DETAIL_FLOW_MAP } from '../../../shared/tour/tours/finance.tours';
import { getPaymentModeLabel, PAYMENT_MODES } from '../../../shared/utils/payment-mode.utils';
import { PermissionService } from '../../../core/permissions/permission.service';
import { transactionReferenceRequiredValidator } from '../../../shared/validators/transaction-reference-validator';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { HttpErrorResponse } from '@angular/common/http';

export interface ReceiptGroup {
  receiptNumber: string;
  paymentDate: string;
  paymentMode: string | null;
  transactionReference: string | null;
  totalAmount: number;
  receiptType: 'PAYMENT' | 'ENQUIRY_PAYMENT' | 'REFUND';
  originalReceiptNumber: string | null;
  lines: Receipt[];
}


@Component({
  selector: 'app-student-fee-detail',
  standalone: true,
  imports: [
    AppDatePipe, PaymentModeLabelPipe, InrPipe, RouterLink, DatePipe, DecimalPipe,
    ReactiveFormsModule, MatTooltipModule, MatProgressSpinnerModule,
    CmsStatusBadgeComponent, CmsRowActionButtonComponent, CashDenominationComponent, FeeReceiptDialogComponent,
    CmsTourButtonComponent,
  ],
  templateUrl: './student-fee-detail.component.html',
  styleUrl: './student-fee-detail.component.scss',
})
export class StudentFeeDetailComponent implements OnInit {
  private readonly route             = inject(ActivatedRoute);
  private readonly finance           = inject(FinanceService);
  private readonly toast             = inject(ToastService);
  private readonly fb                = inject(FormBuilder);
  private readonly tourService       = inject(TourService);
  private readonly permissionService = inject(PermissionService);

  // ── Data signals ──────────────────────────────────────────────────────────────
  protected readonly loading            = signal(true);
  protected readonly initializing       = signal(false);
  protected readonly initError          = signal(false);
  protected readonly allocation         = signal<StudentFeeAllocation | null>(null);
  protected readonly receiptGroups      = signal<ReceiptGroup[]>([]);
  protected readonly creditApplications = signal<EnquiryCreditApplication[]>([]);

  // ── Payment form signals ──────────────────────────────────────────────────────
  protected readonly showConfirmModal  = signal(false);
  protected readonly historyOpen   = signal(false);
  protected readonly saving            = signal(false);
  protected readonly denominationValid = signal(false);
  protected readonly receipt           = signal<ReceiptDisplayData | null>(null);
  protected readonly advanceMode       = signal(false);

  protected readonly paymentModes = PAYMENT_MODES;
  protected readonly getPaymentModeLabel = getPaymentModeLabel;

  protected readonly form: FormGroup = this.fb.group({
    amount:               [null, [Validators.required, Validators.min(1)]],
    paymentDate:          ['',   Validators.required],
    paymentMode:          ['',   Validators.required],
    transactionReference: ['',   [transactionReferenceRequiredValidator('paymentMode')]],
    remarks:              [''],
    allowExcess:          [false],
  });

  protected readonly canAllowExcess = computed(() => this.permissionService.has('FEE_COLLECT_EXCESS'));

  // ── Computed: installment rows (flat, for the grid table) ─────────────────────
  protected readonly semesterRows = computed(() => {
    const sems = this.allocation()?.installmentFees ?? [];
    const nextIndex = sems.findIndex(s => s.pendingAmount > 0);
    return sems.map((s, i) => ({
      label:       s.installmentLabel,
      fee:         s.amount,
      paid:        s.amountPaid,
      outstanding: s.pendingAmount,
      dueDate:     s.dueDate,
      isPaid:      s.pendingAmount === 0,
      isNext:      i === nextIndex,
    }));
  });

  // ── Computed: totals ──────────────────────────────────────────────────────────
  protected readonly totalFee = computed(() =>
    this.semesterRows().reduce((s, r) => s + r.fee, 0)
  );
  protected readonly totalPaid = computed(() =>
    this.semesterRows().reduce((s, r) => s + r.paid, 0)
  );
  protected readonly totalOutstanding = computed(() =>
    this.semesterRows().reduce((s, r) => s + r.outstanding, 0)
  );
  protected readonly collectibleNowOutstanding = computed(() =>
    (this.allocation()?.installmentFees ?? [])
      .filter(s => s.collectibleNow)
      .reduce((sum, s) => sum + s.pendingAmount, 0)
  );
  protected readonly hasAdvanceScope = computed(() =>
    this.totalOutstanding() > 0 && this.totalOutstanding() > this.collectibleNowOutstanding()
  );
  protected readonly amountMax = computed<number | null>(() => {
    if (this.semesterRows().length === 0) return null;
    return this.advanceMode() ? this.totalOutstanding() : this.collectibleNowOutstanding();
  });
  protected readonly nextDueSemester = computed(() =>
    this.allocation()?.installmentFees.find(sf => sf.pendingAmount > 0) ?? null
  );

  protected readonly paymentGroups = computed<ReceiptGroup[]>(() =>
    this.receiptGroups().filter(g => g.receiptType !== 'REFUND')
      .sort((a, b) => new Date(b.paymentDate).getTime() - new Date(a.paymentDate).getTime())
  );

  protected readonly refundGroups = computed<ReceiptGroup[]>(() =>
    this.receiptGroups().filter(g => g.receiptType === 'REFUND')
      .sort((a, b) => new Date(b.paymentDate).getTime() - new Date(a.paymentDate).getTime())
  );

  protected readonly grossPaid = computed<number>(() =>
    this.paymentGroups().reduce((s, g) => s + g.totalAmount, 0)
  );

  protected readonly refundTotal = computed<number>(() =>
    this.refundGroups().reduce((s, g) => s + Math.abs(g.totalAmount), 0)
  );

  protected readonly allReceiptGroupsSorted = computed<ReceiptGroup[]>(() =>
    [...this.receiptGroups()].sort((a, b) => new Date(b.paymentDate).getTime() - new Date(a.paymentDate).getTime())
  );

  protected readonly totalsTooltip = computed<string>(() => {
    if (!this.refundGroups().length) return '';
    const fmt = (n: number) => '₹' + n.toLocaleString('en-IN');
    return `Paid: +${fmt(this.grossPaid())}  ·  Refund: −${fmt(this.refundTotal())}`;
  });

  protected absAmount(g: ReceiptGroup): number {
    return Math.abs(g.totalAmount);
  }

  protected stageLabelFor(g: ReceiptGroup): string {
    return g.receiptType === 'ENQUIRY_PAYMENT' ? 'Pre-Admission' : 'Post-Admission';
  }

  protected installmentSummaryFor(g: ReceiptGroup): string {
    if (g.receiptType === 'ENQUIRY_PAYMENT') return 'Pre-Admission Payment';
    const labels = g.lines.map(l => l.installmentLabel).filter(Boolean);
    return labels.length > 0 ? labels.join(', ') : '—';
  }

  // Where the "Back" link returns to — set from the entry point (Collect Payment vs Fee
  // Explorer) so cancelling or finishing a payment doesn't strand the user on the wrong screen.
  protected readonly backRoute       = signal('/student-fees');
  protected readonly backQueryParams = signal<Record<string, string>>({});

  private studentId!: number;

  constructor() {
    effect(() => {
      this.semesterRows();
      this.updateAmountValidators();
    });

    this.form.get('paymentMode')?.valueChanges.subscribe(() => {
      this.form.get('transactionReference')?.updateValueAndValidity();
      if (!this.isExcessEligibleMode() && this.form.get('allowExcess')?.value) {
        this.form.get('allowExcess')?.setValue(false, { emitEvent: false });
      }
      this.updateAmountValidators();
    });

    this.form.get('allowExcess')?.valueChanges.subscribe(() => {
      this.updateAmountValidators();
    });
  }

  ngOnInit(): void {
    this.studentId = Number(this.route.snapshot.paramMap.get('studentId'));
    this.tourService.register('student-fee-detail', STUDENT_FEE_DETAIL_TOUR);
    this.tourService.registerFlowMap('student-fee-detail', STUDENT_FEE_DETAIL_FLOW_MAP);
    this.resolveBackTarget();
    this.loadAll();
  }

  private resolveBackTarget(): void {
    const qp = this.route.snapshot.queryParamMap;
    if (qp.get('returnTo') !== 'fee-collection') return;

    this.backRoute.set('/fee-collection');
    const restored: Record<string, string> = {};
    for (const key of ['search', 'type', 'status']) {
      const value = qp.get(key);
      if (value) restored[key] = value;
    }
    this.backQueryParams.set(restored);
  }

  protected isOverdue(dueDate: string | null): boolean {
    return !!dueDate && new Date(dueDate) < new Date();
  }

  // ── Payment form ──────────────────────────────────────────────────────────────
  protected toggleAdvanceMode(): void {
    const next = !this.advanceMode();
    this.advanceMode.set(next);
    if (!next) this.form.get('allowExcess')?.setValue(false, { emitEvent: false });
    this.updateAmountValidators();
    const prefillAmt = next ? this.totalOutstanding() : this.collectibleNowOutstanding();
    if (prefillAmt > 0) this.form.patchValue({ amount: prefillAmt });
  }

  /** FEE_COLLECT_EXCESS is restricted to bank-rail modes (bank loan disbursements, DD/NEFT/RTGS/IMPS). */
  protected isExcessEligibleMode(): boolean {
    const mode = this.form.get('paymentMode')?.value;
    return mode === 'DEMAND_DRAFT' || mode === 'BANK_TRANSFER';
  }

  protected showExcessOption(): boolean {
    return this.advanceMode() && this.canAllowExcess() && this.isExcessEligibleMode();
  }

  /** Rupees above total outstanding that will become a non-cancellable auto-refund. */
  protected excessPreviewAmount(): number {
    if (!this.form.get('allowExcess')?.value) return 0;
    const max = this.amountMax();
    const raw = this.form.get('amount')?.value;
    if (max === null || raw === null || raw === '') return 0;
    const n = Number(raw);
    return !Number.isNaN(n) && n > max ? n - max : 0;
  }

  protected cancelForm(): void {
    this.form.reset();
    this.advanceMode.set(false);
    this.denominationValid.set(false);
    this.prefillForm();
  }

  protected isCashMode(): boolean {
    return this.form.get('paymentMode')?.value === 'CASH';
  }

  protected isTransactionRefRequired(): boolean {
    const mode = this.form.get('paymentMode')?.value;
    return ['UPI', 'BANK_TRANSFER', 'CHEQUE', 'DEMAND_DRAFT'].includes(mode);
  }

  protected getAmountMaxError(): string | null {
    const maxError = this.form.get('amount')?.errors?.['amountExceedsOutstanding'];
    if (!maxError) return null;
    return `Amount cannot exceed total outstanding of ₹${Number(maxError.max).toLocaleString('en-IN')}`;
  }

  protected onSubmit(): void {
    if (this.form.invalid) { scrollToFirstInvalid(this.form); return; }
    if (this.isCashMode() && !this.denominationValid()) return;
    this.showConfirmModal.set(true);
  }

  protected confirmAndCollect(): void {
    const v = this.form.value;
    const req: CollectPaymentRequest = {
      amount:               v.amount,
      paymentDate:          v.paymentDate,
      paymentMode:          v.paymentMode,
      transactionReference: v.transactionReference?.trim() || undefined,
      remarks:              v.remarks?.trim() || undefined,
      allowExcess:          this.showExcessOption() && !!v.allowExcess || undefined,
    };

    this.saving.set(true);
    this.showConfirmModal.set(false);

    const collect$ = this.advanceMode()
      ? this.finance.collectAdvancePayment(this.studentId, req)
      : this.finance.collectPayment(this.studentId, req);

    collect$.subscribe({
      next: (res) => {
        this.saving.set(false);
        const alloc = this.allocation();
        this.receipt.set({
          receiptNumber:        res.receiptNumber,
          payerType:            'STUDENT',
          payerName:            res.studentName,
          payerIdentifier:      res.rollNumber,
          programName:          alloc?.programName ?? null,
          amountPaid:           Number(res.amountPaid),
          paymentDate:          String(res.paymentDate),
          paymentMode:          String(res.paymentMode),
          transactionReference: res.transactionReference,
          remarks:              res.remarks,
          feeCategory:          res.feeCategory,
          installmentsCovered:  res.installmentBreakdown.map(i => i.installmentLabel).join(', '),
          installmentBreakdown: res.installmentBreakdown.map(i => ({
            label:  i.installmentLabel,
            amount: Number(i.amountApplied),
          })),
        });
      },
      error: (err: unknown) => {
        const httpError = err as HttpErrorResponse;
        this.toast.error((httpError?.error?.message as string) || 'Failed to collect payment');
        this.saving.set(false);
      },
    });
  }

  protected doneWithReceipt(): void {
    this.receipt.set(null);
    this.form.reset();
    this.advanceMode.set(false);
    this.denominationValid.set(false);
    this.loadAll();
  }

  protected retryInit(): void {
    this.initError.set(false);
    this.autoInitializeAllocation();
  }

  // ── Receipt actions ───────────────────────────────────────────────────────────
  protected viewReceipt(group: ReceiptGroup): void {
    void printFeeReceipt(this.toReceiptPrintData(group));
  }

  protected downloadReceipt(group: ReceiptGroup): void {
    void downloadFeeReceipt(this.toReceiptPrintData(group));
  }

  // ── Data loading ──────────────────────────────────────────────────────────────
  private prefillForm(): void {
    const collectible = this.allocation()?.installmentFees.find(s => s.collectibleNow && s.pendingAmount > 0);
    const prefill = collectible ? collectible.pendingAmount : this.collectibleNowOutstanding();
    this.form.patchValue({
      paymentDate: new Date().toISOString().split('T')[0],
      amount: prefill > 0 ? prefill : null,
    });
  }

  private loadAll(): void {
    this.loading.set(true);
    this.initError.set(false);

    this.finance.getFeeAllocationStatus(this.studentId).subscribe({
      next: (data) => {
        this.allocation.set(data);
        this.loading.set(false);
        this.prefillForm();
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

    this.finance.getCreditApplications(this.studentId).subscribe({
      next: (data) => this.creditApplications.set(data),
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

  private toReceiptPrintData(group: ReceiptGroup) {
    const alloc = this.allocation();
    return {
      receiptNumber:        group.receiptNumber,
      payerName:            alloc?.studentName ?? group.lines[0]?.studentName ?? '',
      payerIdentifier:      alloc?.rollNumber  ?? group.lines[0]?.rollNumber  ?? '',
      programName:          alloc?.programName ?? '',
      amountPaid:           Math.abs(group.totalAmount),
      paymentDate:          group.paymentDate,
      paymentMode:          group.paymentMode ?? '',
      transactionReference: group.transactionReference || null,
      feeCategory:          group.lines[0]?.feeCategory ?? null,
      installmentBreakdown: group.lines.map(l => ({
        installmentLabel: l.installmentLabel ?? '',
        amountApplied:    Math.abs(l.amountPaid),
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
          receiptNumber:         r.receiptNumber,
          paymentDate:           r.paymentDate,
          paymentMode:           r.paymentMode,
          transactionReference:  r.transactionReference,
          totalAmount:           r.amountPaid,
          receiptType:           r.receiptType,
          originalReceiptNumber: r.originalReceiptNumber,
          lines: [r],
        });
      }
    }
    return Array.from(map.values());
  }

  private updateAmountValidators(): void {
    const amountControl = this.form.get('amount');
    if (!amountControl) return;

    const validators = [Validators.required, Validators.min(1)];
    const max = this.amountMax();
    const excessAllowed = this.showExcessOption() && !!this.form.get('allowExcess')?.value;
    if (max !== null && !excessAllowed) validators.push(this.maxOutstandingValidator(max));

    amountControl.setValidators(validators);
    amountControl.updateValueAndValidity({ emitEvent: false });
  }

  private maxOutstandingValidator(max: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const raw = control.value;
      if (raw === null || raw === '' || raw === undefined) return null;
      const n = Number(raw);
      if (Number.isNaN(n) || n <= max) return null;
      return { amountExceedsOutstanding: { max, actual: n } };
    };
  }
}
