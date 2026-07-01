import {
  AfterViewInit, Component, computed, inject, OnDestroy, OnInit, signal, ViewChild,
} from '@angular/core';
import {
  FormBuilder, FormGroup, ReactiveFormsModule, Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';

import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { SettingsService } from '../../settings/settings.service';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { PaymentModeLabelPipe } from '../../../shared/pipes/payment-mode-label.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { ExportButtonComponent, ExportFormat } from '../../../shared/export-button';
import { CommissionExplorerService } from '../commission-explorer.service';
import {
  CommissionRecord,
  COMMISSION_STATUS_OPTIONS,
  COMMISSION_SOURCE_OPTIONS,
} from '../commission-explorer.model';
import { PAYMENT_MODES } from '../../../shared/utils/payment-mode.utils';

const DEFAULT_PAGE_SIZE = 25;

@Component({
  selector: 'app-commission-explorer-list',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    InrPipe, AppDatePipe, PaymentModeLabelPipe,
    CmsEmptyStateComponent, CmsRowActionButtonComponent, CmsTypeBadgeComponent,
    ExportButtonComponent,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatTooltipModule, MatProgressSpinnerModule,
  ],
  templateUrl: './commission-explorer-list.component.html',
  styleUrl:    './commission-explorer-list.component.scss',
})
export class CommissionExplorerListComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly explorerService  = inject(CommissionExplorerService);
  private readonly permService      = inject(PermissionService);
  private readonly toast            = inject(ToastService);
  private readonly fb               = inject(FormBuilder);
  private readonly settingsService  = inject(SettingsService);
  private readonly router           = inject(Router);
  private readonly route            = inject(ActivatedRoute);

  @ViewChild(MatPaginator) paginator?: MatPaginator;

  protected readonly canManage = computed(() => this.permService.has('COMMISSION_MANAGE'));
  protected readonly canSettle = computed(() => this.permService.has('COMMISSION_SETTLE'));
  protected readonly canExport = computed(() => this.permService.has('COMMISSION_EXPORT'));

  protected readonly displayedColumns = [
    'studentName', 'referrer', 'program', 'commissionAmount',
    'paidAmount', 'outstanding', 'status', 'actions',
  ];

  protected readonly dataSource    = new MatTableDataSource<CommissionRecord>([]);
  protected readonly statusOptions = COMMISSION_STATUS_OPTIONS;
  protected readonly sourceOptions = COMMISSION_SOURCE_OPTIONS;
  protected readonly paymentModes  = PAYMENT_MODES;

  // ── OneBook config ────────────────────────────────────────────────────────────
  protected readonly oneBookEnabled = signal(false);

  // ── State ────────────────────────────────────────────────────────────────────
  protected readonly loading       = signal(false);
  protected readonly exporting     = signal(false);
  protected readonly actionLoading = signal(false);

  // ── Filters (synced from URL params) ──────────────────────────────────────────
  protected readonly filterSearch = signal('');
  protected readonly filterStatus = signal('');
  protected readonly filterSource = signal('');
  protected readonly filterFrom   = signal('');
  protected readonly filterTo     = signal('');

  // ── Server-side pagination state ──────────────────────────────────────────────
  protected totalElements   = 0;
  private currentPage       = 0;
  private currentPageSize   = DEFAULT_PAGE_SIZE;

  protected readonly hasFilters = computed(() =>
    !!this.filterSearch() || !!this.filterStatus() || !!this.filterSource() ||
    !!this.filterFrom() || !!this.filterTo());

  // ── Page-level summary stats (computed from current page) ────────────────────
  protected totalDue             = 0;
  protected totalPaid            = 0;
  protected totalOutstanding     = 0;
  protected totalAwaitingApproval = 0;

  // ── Expanded row ─────────────────────────────────────────────────────────────
  protected readonly expandedRow = signal<CommissionRecord | null>(null);

  // ── Payout modal ─────────────────────────────────────────────────────────────
  protected readonly payoutTarget = signal<CommissionRecord | null>(null);
  protected readonly payoutForm: FormGroup = this.fb.group({
    payoutDate:           ['', [Validators.required]],
    paymentMode:          [null as string | null, [Validators.required]],
    transactionReference: [''],
    remarks:              [''],
  });

  // ── Reject modal ──────────────────────────────────────────────────────────────
  protected readonly rejectTarget       = signal<CommissionRecord | null>(null);
  protected readonly rejectReason       = signal('');
  protected readonly rejectReasonTouched = signal(false);

  // ── Approve confirmation modal ───────────────────────────────────────────────
  protected readonly approveTarget = signal<CommissionRecord | null>(null);

  private readonly destroy$      = new Subject<void>();
  private readonly searchSubject = new Subject<string>();

  ngOnInit(): void {
    this.loadOneBookConfig();

    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.filterSearch.set(params['search'] ?? '');
      this.filterStatus.set(params['status'] ?? '');
      this.filterSource.set(params['source'] ?? '');
      this.filterFrom.set(params['fromDate'] ?? '');
      this.filterTo.set(params['toDate'] ?? '');
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

  private loadOneBookConfig(): void {
    this.settingsService.getByCategory('INTEGRATION').subscribe({
      next: (configs) => {
        const get = (key: string) => configs.find(c => c.configKey === key)?.configValue ?? '';
        this.oneBookEnabled.set(get('onebook.enabled') === 'true');
      },
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    this.explorerService.getAllPage({
      search:      this.filterSearch().length >= 2 ? this.filterSearch() : undefined,
      status:      this.filterStatus() || undefined,
      source:      this.filterSource() || undefined,
      fromDate:    this.filterFrom() || undefined,
      toDate:      this.filterTo() || undefined,
      page:        this.currentPage,
      size:        this.currentPageSize,
    }).subscribe({
      next: page => {
        this.dataSource.data  = page.content;
        this.totalElements    = page.totalElements;
        if (this.paginator) {
          this.paginator.length    = page.totalElements;
          this.paginator.pageIndex = page.number;
          this.paginator.pageSize  = page.size;
        }
        // Recompute page-level stats
        this.totalDue              = page.content.reduce((s, r) => s + (r.commissionAmount ?? 0), 0);
        this.totalPaid             = page.content.reduce((s, r) => s + (r.commissionPaidAmount ?? 0), 0);
        this.totalOutstanding      = page.content.reduce((s, r) => s + (r.commissionOutstanding ?? 0), 0);
        this.totalAwaitingApproval = page.content.filter(r => r.commissionPaymentStatus === 'PENDING').length;
        this.loading.set(false);
      },
      error: (err) => {
        this.toast.error(this.apiError(err, 'Failed to load commission records'));
        this.loading.set(false);
      },
    });
  }

  protected onSearch(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.filterSearch.set(val);
    this.searchSubject.next(val);
  }

  protected clearSearch(): void {
    this.filterSearch.set('');
    this.searchSubject.next('');
  }

  protected clearFilters(): void {
    this.navigate({ search: null, status: null, source: null, fromDate: null, toDate: null, page: 0 });
  }

  protected navigate(patch: Partial<{
    search: string | null; status: string | null; source: string | null;
    fromDate: string | null; toDate: string | null; page: number; size: number;
  }>): void {
    const cur = this.route.snapshot.queryParams;
    const merged = {
      search:  'search'  in patch ? patch.search  : (cur['search'] ?? null),
      status:  'status'  in patch ? patch.status  : (cur['status'] ?? null),
      source:  'source'  in patch ? patch.source  : (cur['source'] ?? null),
      fromDate:'fromDate'in patch ? patch.fromDate : (cur['fromDate'] ?? null),
      toDate:  'toDate'  in patch ? patch.toDate  : (cur['toDate'] ?? null),
      page:    'page'    in patch ? patch.page    : this.currentPage,
      size:    'size'    in patch ? patch.size    : this.currentPageSize,
    };
    const queryParams = Object.fromEntries(
      Object.entries(merged).filter(([, v]) => v !== null && v !== undefined && v !== ''),
    );
    void this.router.navigate([], { relativeTo: this.route, queryParams });
  }

  protected onExport(format: ExportFormat): void {
    if (this.exporting()) return;
    this.exporting.set(true);
    this.explorerService.exportCommissions(format, {
      search:   this.filterSearch().trim() || null,
      status:   this.filterStatus() || null,
      source:   this.filterSource() || null,
      fromDate: this.filterFrom() || null,
      toDate:   this.filterTo() || null,
    }).subscribe({
      next: (blob) => {
        const ext = format === 'pdf' ? 'pdf' : 'xlsx';
        const url = URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href     = url;
        a.download = `commissions.${ext}`;
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

  protected toggleRow(row: CommissionRecord): void {
    this.expandedRow.set(this.expandedRow() === row ? null : row);
  }

  protected isExpanded(row: CommissionRecord): boolean {
    return this.expandedRow() === row;
  }

  // ── Status helpers ────────────────────────────────────────────────────────────
  protected statusLabel(s: string | null): string {
    const map: Record<string, string> = {
      PENDING:           'Pending',
      PAYMENT_REQUESTED: 'Awaiting Payment',
      PARTIAL:           'Partial',
      PAID:              'Paid',
      NOT_APPLICABLE:    'N/A',
      TRANSMITTED:       'Transmitted',
      PROCESSING:        'Processing',
      FAILED:            'Failed',
      REJECTED:          'Rejected',
    };
    return s ? (map[s] ?? s) : '—';
  }

  protected statusClass(s: string | null): string {
    const map: Record<string, string> = {
      PENDING:           'badge--warning',
      PAYMENT_REQUESTED: 'badge--info',
      PARTIAL:           'badge--partial',
      PAID:              'badge--success',
      NOT_APPLICABLE:    'badge--neutral',
      TRANSMITTED:       'badge--transmitted',
      PROCESSING:        'badge--processing',
      FAILED:            'badge--danger',
      REJECTED:          'badge--danger',
    };
    return s ? (map[s] ?? '') : '';
  }

  protected referrerLabel(r: CommissionRecord): string {
    return r.agentName ?? r.staffReferrerName ?? r.referredFacultyName ?? r.referralTypeName ?? '—';
  }

  protected referrerType(r: CommissionRecord): string {
    const map: Record<string, string> = {
      AGENT:            'Agent',
      STAFF_REFERRER:   'Staff',
      FACULTY_REFERRER: 'Faculty',
      REFERRAL_TYPE:    'Type',
      NONE:             '',
    };
    return r.commissionSource ? (map[r.commissionSource] ?? '') : '';
  }

  // ── Approve ───────────────────────────────────────────────────────────────────
  protected openApproveModal(r: CommissionRecord, event: Event): void {
    event.stopPropagation();
    this.approveTarget.set(r);
  }

  protected closeApproveModal(): void {
    this.approveTarget.set(null);
  }

  protected confirmApprove(): void {
    const target = this.approveTarget();
    if (!target) return;

    this.actionLoading.set(true);
    this.explorerService.approve(target.enquiryId).subscribe({
      next: () => {
        this.toast.success(this.oneBookEnabled() ? 'Payment transmitted to OneBook' : 'Commission approved — awaiting payment');
        this.closeApproveModal();
        this.actionLoading.set(false);
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(this.apiError(err, 'Failed to approve commission'));
        this.actionLoading.set(false);
      },
    });
  }

  // ── Retry OneBook transmission (FAILED only) ─────────────────────────────────
  protected retryOneBook(r: CommissionRecord, event: Event): void {
    event.stopPropagation();
    this.actionLoading.set(true);
    this.explorerService.approve(r.enquiryId).subscribe({
      next: () => {
        this.toast.success('Payment transmitted to OneBook');
        this.actionLoading.set(false);
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(this.apiError(err, 'Failed to transmit payment to OneBook'));
        this.actionLoading.set(false);
      },
    });
  }

  // ── Reject ────────────────────────────────────────────────────────────────────
  protected openRejectModal(r: CommissionRecord, event: Event): void {
    event.stopPropagation();
    this.rejectReason.set('');
    this.rejectTarget.set(r);
  }

  protected closeRejectModal(): void {
    this.rejectTarget.set(null);
    this.rejectReason.set('');
    this.rejectReasonTouched.set(false);
  }

  protected submitReject(): void {
    const target = this.rejectTarget();
    const reason = this.rejectReason().trim();
    if (!reason) { this.rejectReasonTouched.set(true); return; }
    if (!target) return;

    this.actionLoading.set(true);
    this.explorerService.reject(target.enquiryId, reason).subscribe({
      next: () => {
        this.toast.success('Commission rejected');
        this.closeRejectModal();
        this.actionLoading.set(false);
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(this.apiError(err, 'Failed to reject commission'));
        this.actionLoading.set(false);
      },
    });
  }

  // ── Reopen a rejected commission ─────────────────────────────────────────────
  protected reopen(r: CommissionRecord, event: Event): void {
    event.stopPropagation();
    this.actionLoading.set(true);
    this.explorerService.reopen(r.enquiryId).subscribe({
      next: () => {
        this.toast.success('Commission reopened — back to pending');
        this.actionLoading.set(false);
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(this.apiError(err, 'Failed to reopen commission'));
        this.actionLoading.set(false);
      },
    });
  }

  // ── Record payout (settlement) ───────────────────────────────────────────────
  protected openPayoutModal(r: CommissionRecord, event: Event): void {
    event.stopPropagation();
    this.payoutForm.reset({
      payoutDate: new Date().toISOString().slice(0, 10),
      paymentMode: null,
      transactionReference: '',
      remarks: '',
    });
    this.payoutTarget.set(r);
  }

  protected closePayoutModal(): void {
    this.payoutTarget.set(null);
    this.payoutForm.reset();
  }

  protected submitPayout(): void {
    if (this.payoutForm.invalid) { this.payoutForm.markAllAsTouched(); return; }
    const target = this.payoutTarget();
    if (!target) return;

    const v = this.payoutForm.value;
    this.actionLoading.set(true);
    this.explorerService.recordPayout(target.enquiryId, {
      amount:               target.commissionAmount,
      payoutDate:           v.payoutDate,
      paymentMode:          v.paymentMode,
      transactionReference: v.transactionReference?.trim() || undefined,
      remarks:              v.remarks?.trim() || undefined,
    }).subscribe({
      next: () => {
        this.toast.success('Payout recorded');
        this.closePayoutModal();
        this.actionLoading.set(false);
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(this.apiError(err, 'Failed to record payout'));
        this.actionLoading.set(false);
      },
    });
  }

  private apiError(err: unknown, fallback: string): string {
    if (err instanceof HttpErrorResponse && err.error?.message) return err.error.message;
    return fallback;
  }
}
