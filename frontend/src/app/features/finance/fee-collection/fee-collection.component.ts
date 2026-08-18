import { Component, computed, effect, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DecimalPipe } from '@angular/common';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { debounceTime, distinctUntilChanged, Subject, takeUntil } from 'rxjs';
import { EnquiryService } from '../../enquiry/enquiry.service';
import { FinanceService } from '../finance.service';
import { Enquiry, EnquiryPaymentRequest, EnquiryYearWiseFeeStatusResponse } from '../../enquiry/enquiry.model';
import { StudentFeeSummary, InstallmentFeeDetail, ReceiptDisplayData } from '../finance.model';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { TourService } from '../../../shared/tour/tour.service';
import { FEE_COLLECTION_TOUR, COLLECT_BALANCE_TOUR, FEE_COLLECTION_FLOW_MAP } from '../../../shared/tour/tours/finance.tours';
import { ToastService } from '../../../core/toast/toast.service';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { scrollToFirstInvalid } from '../../../shared/utils/scroll-to-invalid';
import { getPaymentModeLabel, PAYMENT_MODES } from '../../../shared/utils/payment-mode.utils';
import { CashDenominationComponent } from '../../../shared/cash-denomination/cash-denomination.component';
import { FeeReceiptDialogComponent } from '../../../shared/fee-receipt-dialog/fee-receipt-dialog.component';
import { transactionReferenceRequiredValidator } from '../../../shared/validators/transaction-reference-validator';
import { pastDateOnlyValidator } from '../../../shared/validators/date.validators';
import { HttpErrorResponse } from '@angular/common/http';
import { ColumnPickerState, CmsColumnPickerComponent } from '../../../shared/column-picker';
import { PermissionService } from '../../../core/permissions/permission.service';

import { ColumnResizeDirective, CmsWrapTextToggleComponent } from '../../../shared/column-resize';
export type FilterType   = 'ALL' | 'ENQUIRY' | 'STUDENT';
export type FilterStatus = 'ALL' | 'OUTSTANDING';
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
  // Sum of every currently-open (collectible-now) installment — the ceiling a single payment
  // may cover. Not shown as its own column; used for gating/prefill/max-cap logic only.
  totalOutstanding: number;
  // True lifetime remaining balance (Total Fee - Paid), independent of term-instance status.
  lifetimeOutstanding: number;
  // Outstanding amount of just the single next unpaid, currently-open installment.
  currentDue: number;
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
    CmsColumnPickerComponent, ColumnResizeDirective, CmsWrapTextToggleComponent,
  ],
  templateUrl: './fee-collection.component.html',
  styleUrl: './fee-collection.component.scss',
})
export class FeeCollectionComponent implements OnInit, OnDestroy {
  private readonly route          = inject(ActivatedRoute);
  private readonly router         = inject(Router);
  private readonly enquiryService = inject(EnquiryService);
  private readonly financeService = inject(FinanceService);
  private readonly toast          = inject(ToastService);
  private readonly fb             = inject(FormBuilder);
  private readonly tourService    = inject(TourService);
  private readonly permissionService = inject(PermissionService);

  @ViewChild(MatTable) private _matTable?: MatTable<unknown>;
  @ViewChild(MatPaginator)
  set paginator(value: MatPaginator | undefined) {
    this._paginator = value;
    this.dataSource.paginator = value ?? null;
  }
  private _paginator?: MatPaginator;

  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  private readonly allEnquiries          = signal<Enquiry[]>([]);
  private readonly currentStudentEntries = signal<FeeEntry[]>([]);
  private readonly destroy$              = new Subject<void>();
  private readonly searchSubject$        = new Subject<string>();

  protected readonly loading          = signal(true);
  protected readonly feeEntries       = computed<FeeEntry[]>(() => [
    ...this.allEnquiries()
      .filter(e => this.canCollectEnquiryBalance(e))
      .map(e => this.enquiryToEntry(e)),
    ...this.currentStudentEntries(),
  ]);
  protected readonly selectedEntry    = signal<FeeEntry | null>(null);
  protected readonly feeStatus        = signal<EnquiryYearWiseFeeStatusResponse | null>(null);
  protected readonly studentSemesters = signal<InstallmentFeeDetail[]>([]);
  protected readonly saving           = signal(false);
  protected readonly denominationValid = signal(false);
  protected readonly receipt          = signal<ReceiptDisplayData | null>(null);
  protected readonly advanceMode      = signal(false);
  protected readonly canCollectAdvance = computed(() => this.permissionService.has('ENQUIRY_FEE_COLLECT_ADVANCE'));

  protected readonly searchTerm   = signal('');
  protected readonly filterType   = signal<FilterType>('ENQUIRY');
  protected readonly filterStatus = signal<FilterStatus>('ALL');

  protected readonly colState = new ColumnPickerState({
    storageKey: 'fee-collection-columns',
    columns: [
      { key: 'name', label: 'Name', mandatory: true },
      { key: 'type', label: 'Type' },
      { key: 'programName', label: 'Program' },
      { key: 'totalFee', label: 'Total Fee' },
      { key: 'totalPaid', label: 'Paid' },
      { key: 'lifetimeOutstanding', label: 'Total Outstanding' },
      { key: 'currentDue', label: 'Current Due' },
      { key: 'actions', label: 'Actions', mandatory: true, pinnable: false },
    ],
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());
  protected readonly dataSource = new MatTableDataSource<FeeEntry>([]);

  protected readonly filteredEntries = computed(() => {
    const term   = this.searchTerm().toLowerCase().trim();
    const type   = this.filterType();
    const status = this.filterStatus();

    return this.feeEntries().filter(e => {
      if (type !== 'ALL' && e.type !== type) return false;
      if (status === 'OUTSTANDING' && !this.hasCollectableOutstanding(e.totalOutstanding)) return false;

      // Student search is server-side; enquiry search is client-side
      if (e.type === 'ENQUIRY' && term) {
        const matchesName    = e.name.toLowerCase().includes(term);
        const matchesRoll    = !!e.rollNumber && e.rollNumber.toLowerCase().includes(term);
        const matchesProgram = e.programName.toLowerCase().includes(term);
        const matchesCourse  = !!e.courseName && e.courseName.toLowerCase().includes(term);
        if (!matchesName && !matchesRoll && !matchesProgram && !matchesCourse) return false;
      }
      return true;
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
    allowExcess:          [false],
  });

  protected readonly semesterRows = computed<Array<{
    label: string; fee: number; paid: number; outstanding: number;
    dueDate: string | null; isPaid: boolean; isNext: boolean; collectibleNow: boolean;
  }>>(() => {
    const entry = this.selectedEntry();
    if (!entry) return [];

    if (entry.type === 'ENQUIRY') {
      const fs = this.feeStatus();
      if (!fs) return [];
      const sems = fs.installmentBreakdown;
      const immediatePayableIndex = sems.findIndex(s =>
        this.hasCollectableOutstanding(s.outstanding) && s.collectibleNow);
      return sems.map((s, i) => ({
        label:          s.installmentLabel,
        fee:            this.normalizeMoney(s.allocatedFee),
        paid:           this.normalizeMoney(s.paidAmount),
        outstanding:    this.normalizeMoney(s.outstanding),
        dueDate:        s.dueDate,
        isPaid:         this.normalizeMoney(s.outstanding) === 0,
        isNext:         i === immediatePayableIndex,
        collectibleNow: s.collectibleNow,
      }));
    } else {
      const sems = this.studentSemesters();
      const immediatePayableIndex = sems.findIndex(s =>
        this.hasCollectableOutstanding(s.pendingAmount) && s.collectibleNow);
      return sems.map((s, i) => ({
        label:          s.installmentLabel,
        fee:            this.normalizeMoney(s.amount),
        paid:           this.normalizeMoney(s.amountPaid),
        outstanding:    this.normalizeMoney(s.pendingAmount),
        dueDate:        s.dueDate,
        isPaid:         this.normalizeMoney(s.pendingAmount) === 0,
        isNext:         i === immediatePayableIndex,
        collectibleNow: s.collectibleNow,
      }));
    }
  });

  protected readonly totalFee         = computed(() => this.semesterRows().reduce((s, r) => s + r.fee, 0));
  protected readonly totalPaid        = computed(() => this.semesterRows().reduce((s, r) => s + r.paid, 0));
  protected readonly totalOutstanding = computed(() => this.semesterRows().reduce((s, r) => s + r.outstanding, 0));
  // Amount actually payable right now — excludes installments whose term hasn't opened yet
  // (e.g. next year's fee), even though they still count toward totalOutstanding above.
  protected readonly collectibleOutstanding = computed(() =>
    this.semesterRows().reduce((s, r) => s + (r.collectibleNow ? r.outstanding : 0), 0));
  // Full remaining course fee (all installments, open or not) — the ceiling advance mode raises
  // the cap to. Only meaningful for ENQUIRY entries; feeStatus() is null for STUDENT (which
  // navigates away to its own page before this form is ever shown, see selectEntry()).
  protected readonly fullCourseOutstanding = computed(() => {
    const fs = this.feeStatus();
    return fs ? this.normalizeMoney(fs.totalOutstanding) : this.collectibleOutstanding();
  });
  protected readonly amountMax = computed<number | null>(() => {
    const rows = this.semesterRows();
    if (rows.length > 0) {
      return this.advanceMode() ? this.fullCourseOutstanding() : this.collectibleOutstanding();
    }
    return null;
  });
  protected readonly installmentDataReady = computed(() => {
    const entry = this.selectedEntry();
    return !entry || this.semesterRows().length > 0;
  });

  protected get hasActiveFilters(): boolean {
    return !!this.searchTerm() || this.filterType() !== 'ENQUIRY' || this.filterStatus() !== 'ALL';
  }

  constructor() {
    effect(() => {
      this.dataSource.data = this.filteredEntries();
    });

    effect(() => {
      this.selectedEntry();
      this.semesterRows();
      this.updateAmountValidators();
    });

    // Trigger validation update when payment mode changes
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

  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }

  ngOnInit(): void {
    this.tourService.register('fee-collection', FEE_COLLECTION_TOUR);
    this.tourService.register('collect-balance', COLLECT_BALANCE_TOUR);
    this.tourService.registerFlowMap('fee-collection', FEE_COLLECTION_FLOW_MAP);
    this.restoreFiltersFromQueryParams();
    this.loadAll(() => this.applyDeepLink());

    // Debounced search: students are server-side (load only when filter includes them);
    // enquiries are client-side so just reset the paginator.
    this.searchSubject$.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(() => {
      if (this.filterType() !== 'ENQUIRY') {
        this.loadAllStudents(() => {
          if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
        });
      } else {
        if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
      }
    });

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

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadAll(onComplete?: () => void): void {
    this.loading.set(true);
    this.enquiryService.getEnquiries().subscribe({
      next: (enquiries) => {
        this.allEnquiries.set(enquiries);
        if (this.filterType() !== 'ENQUIRY') {
          this.loadAllStudents(() => {
            this.loading.set(false);
            onComplete?.();
          });
        } else {
          this.loading.set(false);
          onComplete?.();
        }
      },
      error: () => {
        this.toast.error('Failed to load fee data');
        this.loading.set(false);
      },
    });
  }

  private loadAllStudents(callback?: () => void): void {
    const search = this.searchTerm();
    this.financeService.searchStudentFees(search.length >= 2 ? search : undefined).subscribe({
      next: (result) => {
        const entries = result.students
          .filter(s => this.hasCollectableOutstanding(s.collectibleOutstanding))
          .map(s => this.studentToEntry(s));
        this.currentStudentEntries.set(entries);
        callback?.();
      },
      error: () => {
        this.toast.error('Failed to load student fees');
        callback?.();
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

  // Restores search/type/status from the URL — used when returning here from a student's
  // Fee Detail page (back link), so the list looks exactly like it did before navigating away.
  private restoreFiltersFromQueryParams(): void {
    const params = this.route.snapshot.queryParamMap;
    const search = params.get('search');
    const type = params.get('type');
    const status = params.get('status');
    if (search) this.searchTerm.set(search);
    if (type === 'ENQUIRY' || type === 'STUDENT' || type === 'ALL') this.filterType.set(type);
    if (status === 'OUTSTANDING' || status === 'ALL') this.filterStatus.set(status);
  }

  // Carries the current filter state along when leaving for a student's Fee Detail page,
  // so the back link there can return here with the list exactly as it was left.
  private buildReturnQueryParams(): Record<string, string> {
    const params: Record<string, string> = { returnTo: 'fee-collection' };
    if (this.searchTerm()) params['search'] = this.searchTerm();
    if (this.filterType() !== 'ENQUIRY') params['type'] = this.filterType();
    if (this.filterStatus() !== 'ALL') params['status'] = this.filterStatus();
    return params;
  }

  private canCollectEnquiryBalance(enquiry: Enquiry): boolean {
    // ADMITTED is the terminal enquiry-side status — there is no separate CONVERTED state.
    // Once the converted student has a finalized allocation, collectibleOutstanding is null
    // (see backend EnquiryService.toResponse), so it's excluded via getEnquiryOutstanding below.
    const blockedStatuses = ['NOT_INTERESTED', 'CANCELLED'];
    return enquiry.finalizedNetFee !== null && enquiry.finalizedNetFee !== undefined &&
      !blockedStatuses.includes(enquiry.status) &&
      this.hasCollectableOutstanding(this.getEnquiryOutstanding(enquiry));
  }

  // Capped to current + past dues — excludes installments whose term hasn't opened yet,
  // even though those still count toward the enquiry's full finalizedNetFee balance.
  private getEnquiryOutstanding(enquiry: Enquiry): number {
    return this.normalizeMoney(enquiry.collectibleOutstanding ?? 0);
  }

  private enquiryToEntry(e: Enquiry): FeeEntry {
    const totalFee = this.normalizeMoney(e.finalizedNetFee ?? 0);
    const totalPaid = this.normalizeMoney(e.totalPaidAmount ?? 0);
    return {
      type: 'ENQUIRY', id: e.id, name: e.name,
      rollNumber: null,
      programName: e.programName ?? '—', courseName: e.courseName,
      totalFee, totalPaid,
      totalOutstanding: this.getEnquiryOutstanding(e),
      lifetimeOutstanding: Math.max(totalFee - totalPaid, 0),
      currentDue: this.normalizeMoney(e.currentInstallmentDue ?? 0),
    };
  }

  private studentToEntry(s: StudentFeeSummary): FeeEntry {
    const totalFee = this.normalizeMoney(s.totalFee);
    const totalPaid = this.normalizeMoney(s.totalPaid);
    return {
      type: 'STUDENT', id: s.studentId, name: s.studentName,
      rollNumber: s.rollNumber ?? null,
      programName: s.programName ?? '—', courseName: null,
      totalFee, totalPaid,
      // Capped to current + past dues — excludes not-yet-open future terms, even though
      // those still count toward totalPending (the full balance shown in Fee Explorer).
      totalOutstanding: this.normalizeMoney(s.collectibleOutstanding),
      lifetimeOutstanding: this.normalizeMoney(s.totalPending),
      currentDue: this.normalizeMoney(s.currentInstallmentDue),
    };
  }

  protected selectEntry(entry: FeeEntry): void {
    if (!this.hasCollectableOutstanding(entry.totalOutstanding)) {
      this.toast.info('No outstanding balance available for this record');
      return;
    }

    if (entry.type === 'STUDENT') {
      void this.router.navigate(['/student-fees', entry.id], {
        queryParams: this.buildReturnQueryParams(),
      });
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
    this.advanceMode.set(false);
    this.form.reset();
    this.form.patchValue({ paymentDate: this.todayIsoDate });

    if (entry.type === 'ENQUIRY') {
      this.enquiryService.getYearWiseFeeStatus(entry.id).subscribe({
        next: (fs) => {
          this.feeStatus.set(fs);
          const sems = fs.installmentBreakdown;
          const nextSem = sems.find(s => this.hasCollectableOutstanding(s.outstanding) && s.collectibleNow);
          const collectibleNow = sems
            .filter(s => s.collectibleNow)
            .reduce((acc, s) => acc + this.normalizeMoney(s.outstanding), 0);
          const prefill = this.normalizeMoney(nextSem ? nextSem.outstanding : collectibleNow);
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
          const nextSem = sems.find(s => this.hasCollectableOutstanding(s.pendingAmount) && s.collectibleNow);
          const collectibleNow = sems
            .filter(s => s.collectibleNow)
            .reduce((acc, s) => acc + this.normalizeMoney(s.pendingAmount), 0);
          const prefill = this.normalizeMoney(nextSem ? nextSem.pendingAmount : collectibleNow);
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

  protected toggleAdvanceMode(): void {
    const next = !this.advanceMode();
    this.advanceMode.set(next);
    if (!next) this.form.get('allowExcess')?.setValue(false, { emitEvent: false });
    this.updateAmountValidators();
    const max = this.amountMax();
    if (max !== null && max > 0) this.form.patchValue({ amount: max });
  }

  /** Excess (beyond the full course fee) is restricted to bank-rail modes, same as the
   *  post-admission Advance Payment flow (FEE_COLLECT_EXCESS). */
  protected isExcessEligibleMode(): boolean {
    const mode = this.form.get('paymentMode')?.value;
    return mode === 'DEMAND_DRAFT' || mode === 'BANK_TRANSFER';
  }

  protected showExcessOption(): boolean {
    return this.advanceMode() && this.canCollectAdvance() && this.isExcessEligibleMode();
  }

  /** Rupees above the full course fee that will become a non-cancellable auto-refund. */
  protected excessPreviewAmount(): number {
    if (!this.form.get('allowExcess')?.value) return 0;
    const max = this.fullCourseOutstanding();
    const raw = this.form.get('amount')?.value;
    if (raw === null || raw === '') return 0;
    const n = Number(raw);
    return !Number.isNaN(n) && n > max ? n - max : 0;
  }

  protected backToList(): void {
    this.selectedEntry.set(null);
    this.feeStatus.set(null);
    this.studentSemesters.set([]);
    this.receipt.set(null);
    this.denominationValid.set(false);
    this.advanceMode.set(false);
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
        allowAdvance:         this.advanceMode() || undefined,
        allowExcess:          (this.showExcessOption() && !!v.allowExcess) || undefined,
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
            feeCategory:         res.feeCategory,
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
    const val = (event.target as HTMLInputElement).value;
    this.searchTerm.set(val);
    this.searchSubject$.next(val);
  }

  protected setFilterType(value: FilterType): void {
    const wasEnquiryOnly = this.filterType() === 'ENQUIRY';
    this.filterType.set(value);
    if (value !== 'ENQUIRY' && wasEnquiryOnly) {
      this.loadAllStudents();
    }
  }

  protected setFilterStatus(value: FilterStatus): void {
    this.filterStatus.set(value);
  }

  protected clearFilters(): void {
    this.searchTerm.set('');
    this.filterType.set('ENQUIRY');
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
    const excessAllowed = this.showExcessOption() && !!this.form.get('allowExcess')?.value;
    if (max !== null && !excessAllowed) {
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
