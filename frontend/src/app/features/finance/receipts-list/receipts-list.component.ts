import {
  AfterViewInit, Component, computed, inject, OnDestroy, OnInit, signal, ViewChild,
} from '@angular/core';
import { ExportFormat } from '../../../shared/export-button/export-button.component';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';

import { FinanceService } from '../finance.service';
import { UnifiedReceiptSummary } from '../finance.model';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { PaymentModeLabelPipe } from '../../../shared/pipes/payment-mode-label.pipe';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ExportButtonComponent } from '../../../shared/export-button/export-button.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { TourService } from '../../../shared/tour/tour.service';
import { RECEIPTS_LIST_TOUR } from '../../../shared/tour/tours/finance.tours';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { PAYMENT_MODES } from '../../../shared/utils/payment-mode.utils';
import { printFeeReceipt, downloadFeeReceipt } from '../../../shared/utils/print-receipt.utils';

const DEFAULT_PAGE_SIZE = 25;

@Component({
  selector: 'app-receipts-list',
  standalone: true,
  imports: [
    FormsModule, ReactiveFormsModule,
    InrPipe, PaymentModeLabelPipe, AppDatePipe,
    CmsEmptyStateComponent, ExportButtonComponent, CmsTourButtonComponent,
    CmsRowActionButtonComponent, CmsTypeBadgeComponent,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatTooltipModule, MatProgressSpinnerModule,
  ],
  templateUrl: './receipts-list.component.html',
  styleUrl: './receipts-list.component.scss',
})
export class ReceiptsListComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly financeService     = inject(FinanceService);
  private readonly toast              = inject(ToastService);
  private readonly permissionService  = inject(PermissionService);
  private readonly fb             = inject(FormBuilder);
  private readonly tourService    = inject(TourService);
  private readonly router         = inject(Router);
  private readonly route          = inject(ActivatedRoute);

  @ViewChild(MatPaginator) paginator?: MatPaginator;

  protected readonly displayedColumns = [
    'paymentDate', 'receiptNumber', 'payer', 'payerId',
    'payerType', 'installmentsCovered', 'paymentMode', 'transactionReference',
    'amountPaid', 'actions',
  ];

  protected readonly dataSource    = new MatTableDataSource<UnifiedReceiptSummary>([]);
  protected readonly paymentModes  = PAYMENT_MODES;
  protected readonly loading       = signal(false);
  protected readonly exporting     = signal(false);
  protected readonly canExport     = computed(() => this.permissionService.has('RECEIPT_EXPORT'));

  // ── Filters ────────────────────────────────────────────────────────────────
  protected readonly searchValue    = signal('');
  protected readonly selectedMode   = signal('');
  protected readonly selectedType   = signal('');
  protected readonly dateFrom       = signal('');
  protected readonly dateTo         = signal('');

  // ── Server-side pagination state ───────────────────────────────────────────
  protected totalElements   = 0;
  private currentPage       = 0;
  private currentPageSize   = DEFAULT_PAGE_SIZE;

  protected readonly hasActiveFilters = computed(() =>
    !!this.searchValue() || !!this.selectedMode() || !!this.selectedType() ||
    !!this.dateFrom() || !!this.dateTo()
  );

  // ── Refund initiation ──────────────────────────────────────────────────────
  protected readonly refundTarget = signal<UnifiedReceiptSummary | null>(null);
  protected readonly refunding    = signal(false);
  protected readonly refundForm: FormGroup = this.fb.group({
    reason: ['', [Validators.required, Validators.minLength(5)]],
  });

  private readonly destroy$      = new Subject<void>();
  private readonly searchSubject = new Subject<string>();

  ngOnInit(): void {
    this.tourService.register('receipts-list', RECEIPTS_LIST_TOUR);

    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.searchValue.set(params['search'] ?? '');
      this.selectedMode.set(params['paymentMode'] ?? '');
      this.selectedType.set(params['payerType'] ?? '');
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

  ngAfterViewInit(): void {
    this.paginator?.page.pipe(takeUntil(this.destroy$)).subscribe((ev: PageEvent) => {
      this.navigate({ page: ev.pageIndex, size: ev.pageSize });
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadPage(): void {
    this.loading.set(true);
    this.financeService.getUnifiedReceiptsPage({
      search:      this.searchValue().length >= 2 ? this.searchValue() : undefined,
      paymentMode: this.selectedMode() || undefined,
      payerType:   this.selectedType() || undefined,
      fromDate:    this.dateFrom() || undefined,
      toDate:      this.dateTo() || undefined,
      page:        this.currentPage,
      size:        this.currentPageSize,
    }).subscribe({
      next: page => {
        this.dataSource.data = page.content;
        this.totalElements   = page.totalElements;
        if (this.paginator) {
          this.paginator.length    = page.totalElements;
          this.paginator.pageIndex = page.number;
          this.paginator.pageSize  = page.size;
        }
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load receipts'); this.loading.set(false); },
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
    this.navigate({ search: null, paymentMode: null, payerType: null, fromDate: null, toDate: null, page: 0 });
  }

  protected canInitiateRefund(r: UnifiedReceiptSummary): boolean {
    return !r.refunded && !r.refundStatus;
  }

  protected getRefundBlockReason(r: UnifiedReceiptSummary): string {
    if (r.refunded || r.refundStatus === 'APPROVED') return 'Already refunded';
    if (r.refundStatus === 'PENDING') return 'Refund approval pending';
    return '';
  }

  protected startRefund(r: UnifiedReceiptSummary): void {
    if (!this.canInitiateRefund(r)) {
      const reason = this.getRefundBlockReason(r);
      if (reason) this.toast.info(`Refund unavailable: ${reason}`);
      return;
    }
    this.refundForm.reset();
    this.refundTarget.set(r);
  }

  protected cancelRefund(): void {
    this.refundTarget.set(null);
    this.refundForm.reset();
  }

  protected confirmRefund(): void {
    if (this.refundForm.invalid) { this.refundForm.markAllAsTouched(); return; }
    const target = this.refundTarget();
    if (!target) return;

    this.refunding.set(true);
    this.financeService.createRefund({
      receiptNumber: target.receiptNumber,
      reason: this.refundForm.value.reason,
    }).subscribe({
      next: () => {
        this.refunding.set(false);
        this.toast.success('Refund request submitted — pending approval');
        this.cancelRefund();
        this.loadPage();
      },
      error: (err) => {
        this.refunding.set(false);
        this.toast.error(this.apiError(err, 'Failed to submit refund request. Please try again.'));
      },
    });
  }

  protected viewReceipt(r: UnifiedReceiptSummary): void {
    void printFeeReceipt({
      receiptNumber:        r.receiptNumber,
      payerName:            r.payerName,
      payerIdentifier:      r.payerIdentifier ?? '',
      admissionNumber:      r.admissionNumber ?? '',
      programName:          r.programName ?? '',
      amountPaid:           r.amountPaid,
      paymentDate:          r.paymentDate,
      paymentMode:          r.paymentMode ?? '',
      transactionReference: r.transactionReference,
      feeCategory:          r.feeCategory,
      installmentBreakdown: r.installmentsCovered
        ? [{ installmentLabel: r.installmentsCovered, amountApplied: r.amountPaid }]
        : [],
    });
  }

  protected downloadReceipt(r: UnifiedReceiptSummary): void {
    void downloadFeeReceipt({
      receiptNumber:        r.receiptNumber,
      payerName:            r.payerName,
      payerIdentifier:      r.payerIdentifier ?? '',
      admissionNumber:      r.admissionNumber ?? '',
      programName:          r.programName ?? '',
      amountPaid:           r.amountPaid,
      paymentDate:          r.paymentDate,
      paymentMode:          r.paymentMode ?? '',
      transactionReference: r.transactionReference,
      feeCategory:          r.feeCategory,
      installmentBreakdown: r.installmentsCovered
        ? [{ installmentLabel: r.installmentsCovered, amountApplied: r.amountPaid }]
        : [],
    });
  }

  protected onExport(format: ExportFormat): void {
    if (this.exporting()) return;
    this.exporting.set(true);
    this.financeService.exportReceipts(format, {
      search:      this.searchValue().length >= 2 ? this.searchValue() : null,
      paymentMode: this.selectedMode() || null,
      payerType:   this.selectedType() || null,
      fromDate:    this.dateFrom() || null,
      toDate:      this.dateTo() || null,
    }).subscribe({
      next: (blob) => {
        const ext = format === 'pdf' ? 'pdf' : 'xlsx';
        const url = URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href     = url;
        a.download = `receipts.${ext}`;
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

  private apiError(err: unknown, fallback: string): string {
    if (err instanceof HttpErrorResponse && err.error?.message) return err.error.message;
    return fallback;
  }

  protected navigate(patch: Partial<{
    search: string | null; paymentMode: string | null; payerType: string | null;
    fromDate: string | null; toDate: string | null; page: number; size: number;
  }>): void {
    const cur = this.route.snapshot.queryParams;
    const merged = {
      search:      'search'      in patch ? patch.search      : (cur['search'] ?? null),
      paymentMode: 'paymentMode' in patch ? patch.paymentMode : (cur['paymentMode'] ?? null),
      payerType:   'payerType'   in patch ? patch.payerType   : (cur['payerType'] ?? null),
      fromDate:    'fromDate'    in patch ? patch.fromDate    : (cur['fromDate'] ?? null),
      toDate:      'toDate'      in patch ? patch.toDate      : (cur['toDate'] ?? null),
      page:        'page'        in patch ? patch.page        : this.currentPage,
      size:        'size'        in patch ? patch.size        : this.currentPageSize,
    };
    const queryParams = Object.fromEntries(
      Object.entries(merged).filter(([, v]) => v !== null && v !== undefined && v !== ''),
    );
    void this.router.navigate([], { relativeTo: this.route, queryParams });
  }
}
