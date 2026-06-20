import { Component, computed, effect, inject, OnInit, signal, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DecimalPipe } from '@angular/common';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { forkJoin } from 'rxjs';
import { EnquiryService } from '../../enquiry/enquiry.service';
import { FinanceService } from '../finance.service';
import { Enquiry, EnquiryPaymentRequest, EnquiryYearWiseFeeStatusResponse } from '../../enquiry/enquiry.model';
import { StudentFeeSummary, InstallmentFeeDetail, ReceiptDisplayData } from '../finance.model';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { TourService } from '../../../shared/tour/tour.service';
import { FEE_COLLECTION_TOUR, COLLECT_BALANCE_TOUR } from '../../../shared/tour/tours/finance.tours';
import { ToastService } from '../../../core/toast/toast.service';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { getPaymentModeLabel, PAYMENT_MODES } from '../../../shared/utils/payment-mode.utils';
import { CashDenominationComponent } from '../../../shared/cash-denomination/cash-denomination.component';
import { FeeReceiptDialogComponent } from '../../../shared/fee-receipt-dialog/fee-receipt-dialog.component';
import { transactionReferenceRequiredValidator } from '../../../shared/validators/transaction-reference-validator';
import { pastDateOnlyValidator } from '../../../shared/validators/date.validators';
import { HttpErrorResponse } from '@angular/common/http';

export type FilterType   = 'ALL' | 'ENQUIRY' | 'STUDENT';
export type FilterStatus = 'ALL' | 'OVERDUE' | 'OUTSTANDING';
export type PersonType   = 'ENQUIRY' | 'STUDENT';

export interface FeeEntry {
  type: PersonType;
  id: number;
  name: string;
  rollNumber: string | null;
  programName: string;
  courseName: string | null;
  totalFee: number;
  totalPaid: number;
  totalOutstanding: number;
  nextDueDate: string | null;
  nextDueLabel: string | null;
}

@Component({
  selector: 'app-fee-collection',
  standalone: true,
  imports: [
    AppDatePipe,
    DecimalPipe,
    InrPipe,
    ReactiveFormsModule,
    MatIconModule, MatProgressSpinnerModule,
    MatTableModule, MatPaginatorModule, MatSortModule, MatTooltipModule,
    CmsEmptyStateComponent,
    CmsTourButtonComponent,
    CmsRowActionButtonComponent,
    CmsTypeBadgeComponent,
    CashDenominationComponent,
    FeeReceiptDialogComponent,
  ],
  templateUrl: './fee-collection.component.html',
  styleUrl: './fee-collection.component.scss',
})
export class FeeCollectionComponent implements OnInit {
  private readonly route          = inject(ActivatedRoute);
  private readonly router         = inject(Router);
  private readonly enquiryService = inject(EnquiryService);
  private readonly financeService = inject(FinanceService);
  private readonly toast          = inject(ToastService);
  private readonly fb             = inject(FormBuilder);
  private readonly tourService    = inject(TourService);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly loading          = signal(true);
  protected readonly feeEntries       = signal<FeeEntry[]>([]);
  protected readonly selectedEntry    = signal<FeeEntry | null>(null);
  protected readonly feeStatus        = signal<EnquiryYearWiseFeeStatusResponse | null>(null);
  protected readonly studentSemesters = signal<InstallmentFeeDetail[]>([]);
  protected readonly saving           = signal(false);
  protected readonly denominationValid = signal(false);
  protected readonly receipt          = signal<ReceiptDisplayData | null>(null);

  protected readonly searchTerm   = signal('');
  protected readonly filterType   = signal<FilterType>('ALL');
  protected readonly filterStatus = signal<FilterStatus>('ALL');

  protected readonly displayedColumns = [
    'name', 'type', 'programName', 'totalFee', 'totalPaid', 'totalOutstanding', 'nextDueDate', 'actions',
  ];
  protected readonly dataSource = new MatTableDataSource<FeeEntry>([]);

  protected readonly filteredEntries = computed(() => {
    const term   = this.searchTerm().toLowerCase().trim();
    const type   = this.filterType();
    const status = this.filterStatus();
    const today  = new Date();

    return this.feeEntries().filter(e => {
      if (term) {
        const matchesName    = e.name.toLowerCase().includes(term);
        const matchesRoll    = !!e.rollNumber && e.rollNumber.toLowerCase().includes(term);
        const matchesProgram = e.programName.toLowerCase().includes(term);
        const matchesCourse  = !!e.courseName && e.courseName.toLowerCase().includes(term);
        if (!matchesName && !matchesRoll && !matchesProgram && !matchesCourse) return false;
      }
      if (type !== 'ALL' && e.type !== type) return false;
      if (status === 'OVERDUE'     && !(e.nextDueDate && new Date(e.nextDueDate) < today)) return false;
      return status !== 'OUTSTANDING' || this.hasCollectableOutstanding(e.totalOutstanding);
    });
  });

  protected readonly paymentModes = PAYMENT_MODES;
  protected readonly getPaymentModeLabel = getPaymentModeLabel;
  protected readonly todayIsoDate = this.toIsoDate(new Date());

  protected readonly form: FormGroup = this.fb.group({
    amount:               [null, [Validators.required, Validators.min(1)]],
    paymentDate:          ['', [Validators.required, pastDateOnlyValidator()]],
    paymentMode:          ['', Validators.required],
    transactionReference: ['', [transactionReferenceRequiredValidator('paymentMode')]],
    remarks:              [''],
  });

  protected readonly semesterRows = computed<Array<{
    label: string; fee: number; paid: number; outstanding: number;
    dueDate: string | null; isPaid: boolean; isNext: boolean;
  }>>(() => {
    const entry = this.selectedEntry();
    if (!entry) return [];

    if (entry.type === 'ENQUIRY') {
      const fs = this.feeStatus();
      if (!fs) return [];
      const sems = fs.installmentBreakdown;
      const immediatePayableIndex = sems.findIndex(s => this.hasCollectableOutstanding(s.outstanding));
      return sems.map((s, i) => ({
        label:       s.installmentLabel,
        fee:         this.normalizeMoney(s.allocatedFee),
        paid:        this.normalizeMoney(s.paidAmount),
        outstanding: this.normalizeMoney(s.outstanding),
        dueDate:     s.dueDate,
        isPaid:      this.normalizeMoney(s.outstanding) === 0,
        isNext:      i === immediatePayableIndex,
      }));
    } else {
      const sems = this.studentSemesters();
      const immediatePayableIndex = sems.findIndex(s => this.hasCollectableOutstanding(s.pendingAmount));
      return sems.map((s, i) => ({
        label:       s.installmentLabel,
        fee:         this.normalizeMoney(s.amount),
        paid:        this.normalizeMoney(s.amountPaid),
        outstanding: this.normalizeMoney(s.pendingAmount),
        dueDate:     s.dueDate,
        isPaid:      this.normalizeMoney(s.pendingAmount) === 0,
        isNext:      i === immediatePayableIndex,
      }));
    }
  });

  protected readonly totalFee         = computed(() => this.semesterRows().reduce((s, r) => s + r.fee, 0));
  protected readonly totalPaid        = computed(() => this.semesterRows().reduce((s, r) => s + r.paid, 0));
  protected readonly totalOutstanding = computed(() => this.semesterRows().reduce((s, r) => s + r.outstanding, 0));
  protected readonly amountMax = computed<number | null>(() => {
    const rows = this.semesterRows();
    if (rows.length > 0) {
      return this.totalOutstanding();
    }
    return null;
  });
  protected readonly installmentDataReady = computed(() => {
    const entry = this.selectedEntry();
    return !entry || this.semesterRows().length > 0;
  });

  protected get hasActiveFilters(): boolean {
    return !!this.searchTerm() || this.filterType() !== 'ALL' || this.filterStatus() !== 'ALL';
  }

  constructor() {
    effect(() => {
      this.dataSource.data = this.filteredEntries();
      if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
    });

    effect(() => {
      this.selectedEntry();
      this.semesterRows();
      this.updateAmountValidators();
    });

    // Trigger validation update when payment mode changes
    this.form.get('paymentMode')?.valueChanges.subscribe(() => {
      this.form.get('transactionReference')?.updateValueAndValidity();
    });
  }

  ngOnInit(): void {
    this.tourService.register('fee-collection', FEE_COLLECTION_TOUR);
    this.tourService.register('collect-balance', COLLECT_BALANCE_TOUR);
    this.loadAll(() => this.applyDeepLink());

    // Subscribe (not snapshot) so browser back/forward updates the view after data is loaded.
    this.route.queryParamMap.subscribe(params => {
      if (this.feeEntries().length === 0) return; // not yet loaded; applyDeepLink handles initial
      const enquiryIdStr = params.get('enquiryId');
      if (!enquiryIdStr) {
        if (this.selectedEntry()?.type === 'ENQUIRY') {
          this.selectedEntry.set(null);
          this.feeStatus.set(null);
          this.studentSemesters.set([]);
          this.receipt.set(null);
          this.denominationValid.set(false);
          this.form.reset();
        }
        return;
      }
      const id = Number(enquiryIdStr);
      if (this.selectedEntry()?.id === id && this.selectedEntry()?.type === 'ENQUIRY') return;
      const entry = this.feeEntries().find(e => e.type === 'ENQUIRY' && e.id === id);
      if (entry) {
        this.applyEntryState(entry);
      } else {
        this.toast.info('This enquiry is not eligible for balance collection');
        void this.router.navigate([], { relativeTo: this.route, queryParams: {} });
      }
    });
  }

  private loadAll(onComplete?: () => void): void {
    this.loading.set(true);
    forkJoin({
      enquiries: this.enquiryService.getEnquiries(),
      students:  this.financeService.searchStudentFees(),
    }).subscribe({
      next: ({ enquiries, students }) => {
        const enquiryEntries: FeeEntry[] = enquiries
          .filter(e => this.canCollectEnquiryBalance(e))
          .map(e => this.enquiryToEntry(e));

        const studentEntries: FeeEntry[] = (students.students ?? [])
              .filter(s => this.hasCollectableOutstanding(s.totalPending))
          .map(s => this.studentToEntry(s));

        const all = [...enquiryEntries, ...studentEntries]
          .filter(entry => this.hasCollectableOutstanding(entry.totalOutstanding))
          .sort((a, b) => {
          if (!a.nextDueDate && !b.nextDueDate) return a.name.localeCompare(b.name);
          if (!a.nextDueDate) return 1;
          if (!b.nextDueDate) return -1;
          return a.nextDueDate.localeCompare(b.nextDueDate);
          });

        this.feeEntries.set(all);
        this.loading.set(false);
        onComplete?.();
      },
      error: () => {
        this.toast.error('Failed to load fee data');
        this.loading.set(false);
      },
    });
  }

  private applyDeepLink(): void {
    const param = this.route.snapshot.queryParamMap.get('enquiryId');
    if (!param) return;
    const id = Number(param);
    const entry = this.feeEntries().find(e => e.type === 'ENQUIRY' && e.id === id);
    if (entry) {
      this.applyEntryState(entry); // state-only, no navigate (URL already has enquiryId)
    } else {
      this.toast.info('This enquiry is not eligible for balance collection');
    }
  }

  private canCollectEnquiryBalance(enquiry: Enquiry): boolean {
    // CONVERTED = student record + fee allocation exist; collection moves to the student side.
    // ADMITTED  = student record created but fee allocation may not exist yet; keep on enquiry side.
    const blockedStatuses = ['NOT_INTERESTED', 'CANCELLED', 'CLOSED', 'CONVERTED'];
    return enquiry.finalizedNetFee !== null && enquiry.finalizedNetFee !== undefined &&
      !blockedStatuses.includes(enquiry.status) &&
      this.hasCollectableOutstanding(this.getEnquiryOutstanding(enquiry));
  }

  private getEnquiryOutstanding(enquiry: Enquiry): number {
    const totalFee = enquiry.finalizedNetFee ?? 0;
    const totalPaid = enquiry.totalPaidAmount ?? 0;
    return this.normalizeMoney(Math.max(0, totalFee - totalPaid));
  }

  private enquiryToEntry(e: Enquiry): FeeEntry {
    const totalFee = this.normalizeMoney(e.finalizedNetFee ?? 0);
    const totalPaid = this.normalizeMoney(e.totalPaidAmount ?? 0);
    return {
      type: 'ENQUIRY', id: e.id, name: e.name,
      rollNumber: null,
      programName: e.programName ?? '—', courseName: e.courseName,
      totalFee, totalPaid,
      totalOutstanding: this.normalizeMoney(Math.max(0, totalFee - totalPaid)),
      nextDueDate: null, nextDueLabel: null,
    };
  }

  private studentToEntry(s: StudentFeeSummary): FeeEntry {
    return {
      type: 'STUDENT', id: s.studentId, name: s.studentName,
      rollNumber: s.rollNumber ?? null,
      programName: s.programName ?? '—', courseName: null,
      totalFee: this.normalizeMoney(s.totalFee),
      totalPaid: this.normalizeMoney(s.totalPaid),
      totalOutstanding: this.normalizeMoney(s.totalPending),
      nextDueDate: null, nextDueLabel: null,
    };
  }

  protected selectEntry(entry: FeeEntry): void {
    if (!this.hasCollectableOutstanding(entry.totalOutstanding)) {
      this.toast.info('No outstanding balance available for this record');
      return;
    }

    if (entry.type === 'STUDENT') {
      void this.router.navigate(['/student-fees', entry.id]);
      return;
    }

    // Update URL so browser back returns to the list, not a previous route.
    void this.router.navigate([], { relativeTo: this.route, queryParams: { enquiryId: entry.id } });
    this.applyEntryState(entry);
  }

  private applyEntryState(entry: FeeEntry): void {
    this.selectedEntry.set(entry);
    this.feeStatus.set(null);
    this.studentSemesters.set([]);
    this.receipt.set(null);
    this.denominationValid.set(false);
    this.form.reset();
    this.form.patchValue({ paymentDate: this.todayIsoDate });

    if (entry.type === 'ENQUIRY') {
      this.enquiryService.getYearWiseFeeStatus(entry.id).subscribe({
        next: (fs) => {
          this.feeStatus.set(fs);
          const sems = fs.installmentBreakdown;
          const nextSem = sems.find(s => this.hasCollectableOutstanding(s.outstanding));
          const prefill = this.normalizeMoney(nextSem ? nextSem.outstanding : fs.totalOutstanding);
          if (!this.form.get('amount')?.value) {
            this.form.patchValue({ amount: this.hasCollectableOutstanding(prefill) ? prefill : null });
          }
        },
        error: () => {
          this.form.patchValue({ amount: this.hasCollectableOutstanding(entry.totalOutstanding) ? entry.totalOutstanding : null });
        },
      });
    } else {
      this.financeService.getFeeAllocationStatus(entry.id).subscribe({
        next: (alloc) => {
          this.studentSemesters.set(alloc.installmentFees);
          const sems = alloc.installmentFees;
          const nextSem = sems.find(s => this.hasCollectableOutstanding(s.pendingAmount));
          const prefill = this.normalizeMoney(
            nextSem ? nextSem.pendingAmount : sems.reduce((acc, sf) => acc + sf.pendingAmount, 0)
          );
          if (!this.form.get('amount')?.value) {
            this.form.patchValue({ amount: this.hasCollectableOutstanding(prefill) ? prefill : null });
          }
        },
        error: () => {
          this.form.patchValue({ amount: this.hasCollectableOutstanding(entry.totalOutstanding) ? entry.totalOutstanding : null });
        },
      });
    }
  }

  protected isCashMode(): boolean {
    return this.form.get('paymentMode')?.value === 'CASH';
  }

  protected backToList(): void {
    this.selectedEntry.set(null);
    this.feeStatus.set(null);
    this.studentSemesters.set([]);
    this.receipt.set(null);
    this.denominationValid.set(false);
    this.form.reset();
    // Clear query param to update browser history correctly.
    void this.router.navigate([], { relativeTo: this.route, queryParams: {} });
  }

  protected onSubmit(): void {
    if (this.form.invalid) { scrollToFirstInvalid(this.form); return; }
    if (this.isCashMode() && !this.denominationValid()) return;
    if (!this.installmentDataReady()) {
      this.toast.info('Please wait for installment details to finish loading');
      return;
    }
    const entry = this.selectedEntry();
    if (!entry) return;
    if (!this.hasCollectableOutstanding(entry.totalOutstanding)) {
      this.toast.info('No outstanding balance available for this record');
      return;
    }

    const v = this.form.value;
    const normalizedAmount = this.normalizeMoney(v.amount);
    this.saving.set(true);

    if (entry.type === 'ENQUIRY') {
      const req: EnquiryPaymentRequest = {
        amountPaid:           normalizedAmount,
        paymentDate:          v.paymentDate,
        paymentMode:          v.paymentMode,
        transactionReference: v.transactionReference || undefined,
        remarks:              v.remarks || undefined,
      };
      this.enquiryService.collectPayment(entry.id, req).subscribe({
        next: (res) => {
          this.saving.set(false);
          const towardsLabel = res.feeCategory === 'TUITION_AND_HOSTEL'
            ? 'Tuition Fees And Hostel Fees' : 'Tuition Fees';
          this.receipt.set({
            receiptNumber:       res.receiptNumber,
            payerType:           'ENQUIRY',
            payerName:           res.enquiryName,
            payerIdentifier:     null,
            programName:         entry.courseName ?? (entry.programName !== '—' ? entry.programName : null),
            amountPaid:          Number(res.amountPaid),
            paymentDate:         String(res.paymentDate),
            paymentMode:         String(res.paymentMode),
            transactionReference: res.transactionReference,
            remarks:             res.remarks,
            feeCategory:         res.feeCategory,
            installmentsCovered: towardsLabel,
            installmentBreakdown: [{ label: towardsLabel, amount: Number(res.amountPaid) }],
          });
        },
        error: (err: unknown) => {
          this.toast.error(this.getApiErrorMessage(err, 'Failed to collect payment'));
          this.saving.set(false);
        },
      });
    } else {
      this.financeService.collectPayment(entry.id, {
        amount: normalizedAmount, paymentDate: v.paymentDate, paymentMode: v.paymentMode,
        transactionReference: v.transactionReference || undefined,
        remarks: v.remarks || undefined,
      }).subscribe({
        next: (res) => {
          this.saving.set(false);
          this.receipt.set({
            receiptNumber:       res.receiptNumber,
            payerType:           'STUDENT',
            payerName:           res.studentName,
            payerIdentifier:     res.rollNumber,
            programName:         entry.programName !== '—' ? entry.programName : null,
            amountPaid:          Number(res.amountPaid),
            paymentDate:         String(res.paymentDate),
            paymentMode:         String(res.paymentMode),
            transactionReference: res.transactionReference,
            remarks:             res.remarks,
            installmentsCovered: res.installmentBreakdown.map(i => i.installmentLabel).join(', '),
            installmentBreakdown: res.installmentBreakdown.map(i => ({
              label: i.installmentLabel,
              amount: Number(i.amountApplied),
            })),
          });
        },
        error: (err: unknown) => {
          this.toast.error(this.getApiErrorMessage(err, 'Failed to collect payment'));
          this.saving.set(false);
        },
      });
    }
  }

  protected doneWithReceipt(): void {
    this.receipt.set(null);
    this.loadAll();
    this.backToList();
  }

  protected onSearch(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  protected setFilterType(value: FilterType): void {
    this.filterType.set(value);
  }

  protected setFilterStatus(value: FilterStatus): void {
    this.filterStatus.set(value);
  }

  protected clearFilters(): void {
    this.searchTerm.set('');
    this.filterType.set('ALL');
    this.filterStatus.set('ALL');
  }

  protected isOverdue(dueDate: string | null): boolean {
    return !!dueDate && new Date(dueDate) < new Date();
  }

  protected isTransactionRefRequired(): boolean {
    const mode = this.form.get('paymentMode')?.value;
    return ['UPI', 'BANK_TRANSFER', 'CHEQUE', 'DEMAND_DRAFT'].includes(mode);
  }

  protected getAmountMaxError(): string | null {
    const amountControl = this.form.get('amount');
    const maxError = amountControl?.errors?.['amountExceedsOutstanding'];
    if (!maxError) return null;

    const formattedMax = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 0 }).format(Number(maxError.max));
    return `Amount cannot exceed total outstanding of ₹${formattedMax}`;
  }

  private getApiErrorMessage(error: unknown, fallback: string): string {
    const httpError = error as HttpErrorResponse;
    return (httpError?.error?.message as string) || fallback;
  }

  private updateAmountValidators(): void {
    const amountControl = this.form.get('amount');
    if (!amountControl) return;

    const validators = [Validators.required, Validators.min(1), this.wholeRupeeAmountValidator()];
    const max = this.amountMax();
    if (max !== null) {
      validators.push(this.maxOutstandingValidator(max));
    }

    amountControl.setValidators(validators);
    amountControl.updateValueAndValidity({ emitEvent: false });
  }

  private maxOutstandingValidator(max: number): ValidatorFn {
    const normalizedMax = this.normalizeMoney(max);
    return (control: AbstractControl): ValidationErrors | null => {
      const rawValue = control.value;
      if (rawValue === null || rawValue === '' || rawValue === undefined) {
        return null;
      }

      const numericValue = Number(rawValue);
      if (Number.isNaN(numericValue) || numericValue <= normalizedMax) {
        return null;
      }

      return {
        amountExceedsOutstanding: {
          max: normalizedMax,
          actual: numericValue,
        },
      };
    };
  }

  private wholeRupeeAmountValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const rawValue = control.value;
      if (rawValue === null || rawValue === '' || rawValue === undefined) {
        return null;
      }

      const numericValue = Number(rawValue);
      if (!Number.isFinite(numericValue) || Number.isInteger(numericValue)) {
        return null;
      }

      return { wholeRupeeOnly: true };
    };
  }

  private hasCollectableOutstanding(value: number | null | undefined): boolean {
    return this.normalizeMoney(value) > 0;
  }

  private normalizeMoney(value: number | null | undefined): number {
    const numericValue = Number(value ?? 0);
    if (Number.isNaN(numericValue)) {
      return 0;
    }
    return Math.max(0, Math.round(numericValue));
  }

  private toIsoDate(value: Date): string {
    return value.toISOString().split('T')[0];
  }
}
