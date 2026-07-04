import {
  Component, computed, inject, ElementRef, OnDestroy, OnInit, signal, ViewChild,
} from '@angular/core';
import { ExportFormat } from '../../../shared/export-button/export-button.component';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TitleCasePipe } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';

import { FinanceService } from '../finance.service';
import { FeeRefundSummary } from '../finance.model';
import { SettingsService } from '../../settings/settings.service';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { PaymentModeLabelPipe } from '../../../shared/pipes/payment-mode-label.pipe';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ExportButtonComponent } from '../../../shared/export-button/export-button.component';
import { CashDenominationComponent } from '../../../shared/cash-denomination/cash-denomination.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { TourService } from '../../../shared/tour/tour.service';
import { FEE_REFUND_LIST_TOUR } from '../../../shared/tour/tours/finance.tours';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { PAYMENT_MODES } from '../../../shared/utils/payment-mode.utils';
import { transactionReferenceRequiredValidator } from '../../../shared/validators/transaction-reference-validator';
import { printRefundVoucher, downloadRefundVoucher, RefundVoucherData } from '../../../shared/utils/print-receipt.utils';
import { CmsIconViewComponent } from '../../../shared/icons';

type PanelMode = 'view' | 'approve' | 'reject';

const DEFAULT_PAGE_SIZE = 25;

@Component({
  selector: 'app-fee-refund-list',
  standalone: true,
  imports: [
    FormsModule, ReactiveFormsModule, TitleCasePipe,
    InrPipe, PaymentModeLabelPipe, AppDatePipe,
    CmsEmptyStateComponent, ExportButtonComponent, CashDenominationComponent, CmsTourButtonComponent,
    CmsRowActionButtonComponent, CmsTypeBadgeComponent,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatTooltipModule, MatProgressSpinnerModule,
    CmsIconViewComponent,
  ],
  templateUrl: './fee-refund-list.component.html',
  styleUrl: './fee-refund-list.component.scss',
})
export class FeeRefundListComponent implements OnInit, OnDestroy {
  private readonly financeService     = inject(FinanceService);
  private readonly settingsService    = inject(SettingsService);
  private readonly toast              = inject(ToastService);
  private readonly permissionService  = inject(PermissionService);
  private readonly fb              = inject(FormBuilder);
  private readonly tourService     = inject(TourService);
  private readonly router          = inject(Router);
  private readonly route           = inject(ActivatedRoute);

  @ViewChild(MatPaginator)
  set paginator(value: MatPaginator | undefined) {
    if (this._paginator === value) return;
    this._paginatorSub?.unsubscribe();
    this._paginator = value;
    if (!value) return;
    this._paginatorSub = value.page.pipe(takeUntil(this.destroy$)).subscribe((ev: PageEvent) => {
      this.navigate({ page: ev.pageIndex, size: ev.pageSize });
    });
    this.syncPaginatorState();
  }
  get paginator(): MatPaginator | undefined { return this._paginator; }
  private _paginator?: MatPaginator;
  private _paginatorSub?: Subscription;
  @ViewChild('panelBody')  private panelBody!: ElementRef<HTMLElement>;

  protected readonly displayedColumns = [
    'requestedAt', 'originalReceiptNumber', 'student', 'programName',
    'refundAmount', 'requestedBy', 'status', 'actions',
  ];

  protected readonly dataSource   = new MatTableDataSource<FeeRefundSummary>([]);
  protected readonly paymentModes = PAYMENT_MODES;

  protected readonly oneBookEnabled    = signal(false);
  protected readonly oneBookAllowCash  = signal(true);

  protected readonly loading           = signal(false);
  protected readonly submittingOneBook = signal(false);
  protected readonly exporting         = signal(false);
  protected readonly canExport         = computed(() => this.permissionService.has('FEE_REFUND_EXPORT'));

  // ── Filters (synced from URL params) ──────────────────────────────────────
  protected readonly searchValue      = signal('');
  protected readonly statusFilter     = signal('');
  protected readonly filterEntityType = signal('');
  protected readonly dateFrom         = signal('');
  protected readonly dateTo           = signal('');

  // ── Server-side pagination state ──────────────────────────────────────────
  protected totalElements   = 0;
  private currentPage       = 0;
  private currentPageSize   = DEFAULT_PAGE_SIZE;

  protected readonly hasActiveFilters = computed(() =>
    !!this.searchValue() || !!this.statusFilter() || !!this.filterEntityType() ||
    !!this.dateFrom() || !!this.dateTo()
  );

  // ── Side panel ─────────────────────────────────────────────────────────────
  protected readonly selectedRefund = signal<FeeRefundSummary | null>(null);
  protected readonly panelMode      = signal<PanelMode>('view');

  // ── Approval form ──────────────────────────────────────────────────────────
  protected readonly approving         = signal(false);
  protected readonly denominationValid = signal(false);
  protected readonly approvalForm: FormGroup = this.fb.group({
    paymentMode:          ['', Validators.required],
    paymentDate:          [this.today, Validators.required],
    transactionReference: ['', transactionReferenceRequiredValidator('paymentMode')],
  });

  // ── Rejection form ─────────────────────────────────────────────────────────
  protected readonly rejecting       = signal(false);
  protected readonly rejectionForm: FormGroup = this.fb.group({
    rejectionReason: ['', [Validators.required, Validators.minLength(5)]],
  });

  private readonly destroy$      = new Subject<void>();
  private readonly searchSubject = new Subject<string>();

  constructor() {
    this.approvalForm.get('paymentMode')?.valueChanges.subscribe(() => {
      this.approvalForm.get('transactionReference')?.updateValueAndValidity();
      this.denominationValid.set(false);
    });
  }

  ngOnInit(): void {
    this.tourService.register('fee-refund-list', FEE_REFUND_LIST_TOUR);
    this.loadOneBookConfig();

    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.searchValue.set(params['search'] ?? '');
      this.statusFilter.set(params['status'] ?? '');
      this.filterEntityType.set(params['entityType'] ?? '');
      this.dateFrom.set(params['fromDate'] ?? '');
      this.dateTo.set(params['toDate'] ?? '');
      this.currentPage     = params['page'] ? +params['page'] : 0;
      this.currentPageSize = params['size'] ? +params['size'] : DEFAULT_PAGE_SIZE;
      this.loadPage();
    });

    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(val => this.navigate({ search: val || null, page: 0 }));
  }

  private syncPaginatorState(): void {
    if (!this._paginator) return;
    this._paginator.length    = this.totalElements;
    this._paginator.pageIndex = this.currentPage;
    this._paginator.pageSize  = this.currentPageSize;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadOneBookConfig(): void {
    this.settingsService.getByCategory('INTEGRATION').subscribe({
      next: settings => {
        const map = Object.fromEntries(settings.map(s => [s.configKey, s.configValue]));
        this.oneBookEnabled.set(map['onebook.enabled'] === 'true');
        this.oneBookAllowCash.set(map['onebook.allow_cash_in_cms'] !== 'false');
      },
    });
  }

  private get today(): string {
    return new Date().toISOString().split('T')[0];
  }

  private loadPage(): void {
    this.loading.set(true);
    this.financeService.getFeeRefundsPage({
      search:     this.searchValue().length >= 2 ? this.searchValue() : undefined,
      status:     this.statusFilter() || undefined,
      entityType: this.filterEntityType() || undefined,
      fromDate:   this.dateFrom() || undefined,
      toDate:     this.dateTo() || undefined,
      page:       this.currentPage,
      size:       this.currentPageSize,
    }).subscribe({
      next: page => {
        this.dataSource.data = page.content;
        this.totalElements   = page.totalElements;
        this.currentPage     = page.number;
        this.currentPageSize = page.size;
        this.syncPaginatorState();
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load refund requests.'); this.loading.set(false); },
    });
  }

  protected onSearch(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.searchValue.set(val);
    this.searchSubject.next(val);
  }

  protected clearSearch(): void {
    this.searchValue.set('');
    this.searchSubject.next('');
  }

  protected clearFilters(): void {
    this.navigate({ search: null, status: null, entityType: null, fromDate: null, toDate: null, page: 0 });
  }

  protected onExport(format: ExportFormat): void {
    if (this.exporting()) return;
    this.exporting.set(true);
    this.financeService.exportRefunds(format, {
      search:     this.searchValue().length >= 2 ? this.searchValue() : null,
      status:     this.statusFilter() || null,
      entityType: this.filterEntityType() || null,
      fromDate:   this.dateFrom() || null,
      toDate:     this.dateTo() || null,
    }).subscribe({
      next: (blob) => {
        const ext = format === 'pdf' ? 'pdf' : 'xlsx';
        const url = URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href     = url;
        a.download = `refunds.${ext}`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting.set(false);
      },
      error: () => {
        this.toast.error('Export failed. Please try again.');
        this.exporting.set(false);
      },
    });
  }

  protected navigate(patch: Partial<{
    search: string | null; status: string | null; entityType: string | null;
    fromDate: string | null; toDate: string | null; page: number; size: number;
  }>): void {
    const cur = this.route.snapshot.queryParams;
    const merged = {
      search:     'search'     in patch ? patch.search     : (cur['search'] ?? null),
      status:     'status'     in patch ? patch.status     : (cur['status'] ?? null),
      entityType: 'entityType' in patch ? patch.entityType : (cur['entityType'] ?? null),
      fromDate:   'fromDate'   in patch ? patch.fromDate   : (cur['fromDate'] ?? null),
      toDate:     'toDate'     in patch ? patch.toDate     : (cur['toDate'] ?? null),
      page:       'page'       in patch ? patch.page       : this.currentPage,
      size:       'size'       in patch ? patch.size       : this.currentPageSize,
    };
    const queryParams = Object.fromEntries(
      Object.entries(merged).filter(([, v]) => v !== null && v !== undefined && v !== ''),
    );
    void this.router.navigate([], { relativeTo: this.route, queryParams });
  }

  // ── Panel ──────────────────────────────────────────────────────────────────

  protected openPanel(r: FeeRefundSummary): void {
    this.selectedRefund.set(r);
    this.panelMode.set('view');
    this.approvalForm.reset({ paymentMode: '', paymentDate: this.today, transactionReference: '' });
    this.rejectionForm.reset();
    this.denominationValid.set(false);
    this.scrollPanelTop();
  }

  protected closePanel(): void {
    this.selectedRefund.set(null);
    this.panelMode.set('view');
  }

  protected startApprove(): void {
    this.approvalForm.patchValue({ paymentMode: this.selectedRefund()?.paymentMode ?? '' });
    this.panelMode.set('approve');
    this.scrollPanelTop();
  }

  protected approveViaOneBook(): void {
    const target = this.selectedRefund();
    if (!target) return;
    this.submittingOneBook.set(true);
    this.financeService.approveRefundViaOneBook(target.id).subscribe({
      next: (res) => {
        this.submittingOneBook.set(false);
        const isFailure = res.status === 'FAILED';
        if (isFailure) {
          this.toast.error('OneBook API rejected the refund — marked as Payment Failed.');
        } else {
          this.toast.success('Refund transmitted to OneBook — awaiting payment confirmation.');
        }
        const updatedStatus = isFailure ? 'PAYMENT_FAILED' : 'TRANSMITTED';
        const updated: FeeRefundSummary = { ...target, status: updatedStatus as FeeRefundSummary['status'] };
        this.selectedRefund.set(updated);
        this.loadPage();
      },
      error: (err) => {
        this.submittingOneBook.set(false);
        this.toast.error(this.apiError(err, 'Failed to transmit refund to OneBook.'));
      },
    });
  }

  protected startReject(): void {
    this.panelMode.set('reject');
    this.scrollPanelTop();
  }

  private scrollPanelTop(): void {
    // Two rAF passes ensure Angular + child components (e.g. cash-denomination) have
    // fully rendered before we reset the scroll position.
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        if (this.panelBody?.nativeElement) {
          this.panelBody.nativeElement.scrollTop = 0;
        }
      });
    });
  }

  protected backToView(): void {
    this.panelMode.set('view');
    this.approvalForm.reset({ paymentMode: '', paymentDate: this.today, transactionReference: '' });
    this.rejectionForm.reset();
    this.denominationValid.set(false);
    this.scrollPanelTop();
  }

  protected isApprovalCashMode(): boolean {
    return this.approvalForm.get('paymentMode')?.value === 'CASH';
  }

  protected confirmApprove(): void {
    if (this.approvalForm.invalid) { this.approvalForm.markAllAsTouched(); return; }
    if (this.isApprovalCashMode() && !this.denominationValid()) return;

    const target = this.selectedRefund();
    if (!target) return;

    this.approving.set(true);
    const v = this.approvalForm.value;
    this.financeService.approveRefund(target.id, {
      paymentMode: v.paymentMode,
      paymentDate: v.paymentDate,
      transactionReference: v.transactionReference || undefined,
    }).subscribe({
      next: (updated) => {
        this.approving.set(false);
        this.toast.success('Refund approved — balance restored and reversal voucher issued');
        this.selectedRefund.set(updated);
        this.panelMode.set('view');
        this.loadPage();
      },
      error: (err) => {
        this.approving.set(false);
        this.toast.error(this.apiError(err, 'Failed to approve refund. Please try again.'));
      },
    });
  }

  protected confirmReject(): void {
    if (this.rejectionForm.invalid) { this.rejectionForm.markAllAsTouched(); return; }
    const target = this.selectedRefund();
    if (!target) return;

    this.rejecting.set(true);
    this.financeService.rejectRefund(target.id, {
      rejectionReason: this.rejectionForm.value.rejectionReason,
    }).subscribe({
      next: (updated) => {
        this.rejecting.set(false);
        this.toast.success('Refund request rejected');
        this.selectedRefund.set(updated);
        this.panelMode.set('view');
        this.loadPage();
      },
      error: (err) => {
        this.rejecting.set(false);
        this.toast.error(this.apiError(err, 'Failed to reject refund. Please try again.'));
      },
    });
  }

  protected viewVoucher(r: FeeRefundSummary): void {
    void printRefundVoucher(this.toVoucherData(r));
  }

  protected downloadVoucher(r: FeeRefundSummary): void {
    void downloadRefundVoucher(this.toVoucherData(r));
  }

  private toVoucherData(r: FeeRefundSummary): RefundVoucherData {
    return {
      refundNumber:          r.refundNumber ?? '',
      originalReceiptNumber: r.originalReceiptNumber,
      payerName:             r.studentName,
      payerIdentifier:       r.rollNumber,
      admissionNumber:       r.admissionNumber,
      programName:           r.programName,
      refundAmount:          r.refundAmount,
      refundDate:            r.paymentDate ?? r.requestedAt.substring(0, 10),
      reason:                r.reason,
      paymentMode:           r.paymentMode,
      paymentDate:           r.paymentDate,
      transactionReference:  r.transactionReference,
    };
  }

  private apiError(err: HttpErrorResponse, fallback: string): string {
    return err?.error?.message ?? fallback;
  }
}
