import {
  Component, computed, effect, inject, OnInit, signal, ViewChild,
} from '@angular/core';
import {
  FormBuilder, FormGroup, ReactiveFormsModule, Validators,
} from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpErrorResponse } from '@angular/common/http';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { SettingsService } from '../../settings/settings.service';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { PaymentModeLabelPipe } from '../../../shared/pipes/payment-mode-label.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { CommissionExplorerService } from '../commission-explorer.service';
import {
  CommissionRecord,
  COMMISSION_STATUS_OPTIONS,
  COMMISSION_SOURCE_OPTIONS,
} from '../commission-explorer.model';
import { PAYMENT_MODES } from '../../../shared/utils/payment-mode.utils';

@Component({
  selector: 'app-commission-explorer-list',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    InrPipe, AppDatePipe, PaymentModeLabelPipe,
    CmsEmptyStateComponent, CmsRowActionButtonComponent, CmsTypeBadgeComponent,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatTooltipModule, MatProgressSpinnerModule,
  ],
  templateUrl: './commission-explorer-list.component.html',
  styleUrl:    './commission-explorer-list.component.scss',
})
export class CommissionExplorerListComponent implements OnInit {
  private readonly explorerService  = inject(CommissionExplorerService);
  private readonly permService      = inject(PermissionService);
  private readonly toast            = inject(ToastService);
  private readonly fb               = inject(FormBuilder);
  private readonly settingsService  = inject(SettingsService);

  @ViewChild(MatPaginator) set paginator(v: MatPaginator) { if (v) this.dataSource.paginator = v; }
  @ViewChild(MatSort)      set sort(v: MatSort)           { if (v) this.dataSource.sort = v; }

  protected readonly canManage = computed(() => this.permService.has('COMMISSION_MANAGE'));

  protected readonly displayedColumns = [
    'studentName', 'referrer', 'program', 'commissionAmount',
    'paidAmount', 'outstanding', 'status', 'actions',
  ];

  protected readonly dataSource   = new MatTableDataSource<CommissionRecord>([]);
  protected readonly statusOptions = COMMISSION_STATUS_OPTIONS;
  protected readonly sourceOptions = COMMISSION_SOURCE_OPTIONS;
  protected readonly paymentModes  = PAYMENT_MODES;

  // ── OneBook config ────────────────────────────────────────────────────────────
  protected readonly oneBookEnabled  = signal(false);
  protected readonly oneBookAllowCash = signal(true);

  // ── State ────────────────────────────────────────────────────────────────────
  protected readonly loading       = signal(false);
  protected readonly actionLoading = signal(false);

  private readonly allRecords = signal<CommissionRecord[]>([]);

  // ── Filters (client-side after initial load) ─────────────────────────────────
  protected readonly filterSearch  = signal('');
  protected readonly filterStatus  = signal('');
  protected readonly filterSource  = signal('');
  protected readonly filterFrom    = signal('');
  protected readonly filterTo      = signal('');

  protected readonly filteredRecords = computed(() => {
    const search = this.filterSearch().trim().toLowerCase();
    const status = this.filterStatus();
    const source = this.filterSource();
    const from   = this.filterFrom();
    const to     = this.filterTo();

    return this.allRecords().filter(r => {
      if (search) {
        const hay = [r.studentName, r.agentName, r.staffReferrerName,
                     r.referredFacultyName, r.admissionNumber, r.referralTypeName]
          .filter(Boolean).join(' ').toLowerCase();
        if (!hay.includes(search)) return false;
      }
      if (status && r.commissionPaymentStatus !== status) return false;
      if (source && r.commissionSource !== source)         return false;
      if (from   && r.enquiryDate < from)                  return false;
      if (to     && r.enquiryDate > to)                    return false;
      return true;
    });
  });

  // ── Summary stats ────────────────────────────────────────────────────────────
  protected readonly totalDue = computed(() =>
    this.filteredRecords().reduce((s, r) => s + (r.commissionAmount ?? 0), 0));
  protected readonly totalPaid = computed(() =>
    this.filteredRecords().reduce((s, r) => s + (r.commissionPaidAmount ?? 0), 0));
  protected readonly totalOutstanding = computed(() =>
    this.filteredRecords().reduce((s, r) => s + (r.commissionOutstanding ?? 0), 0));
  protected readonly totalRequested = computed(() =>
    this.filteredRecords().filter(r => r.commissionPaymentStatus === 'PAYMENT_REQUESTED').length);

  protected readonly hasFilters = computed(() =>
    !!this.filterSearch() || !!this.filterStatus() || !!this.filterSource() ||
    !!this.filterFrom() || !!this.filterTo());

  // ── Expanded row ─────────────────────────────────────────────────────────────
  protected readonly expandedRow = signal<CommissionRecord | null>(null);

  // ── Payout modal ─────────────────────────────────────────────────────────────
  protected readonly payoutTarget  = signal<CommissionRecord | null>(null);
  protected readonly isCashOnly    = signal(false);
  protected readonly payoutForm: FormGroup = this.fb.group({
    amount:               [null as number | null, [Validators.required, Validators.min(0.01)]],
    payoutDate:           ['', [Validators.required]],
    paymentMode:          [null as string | null, [Validators.required]],
    transactionReference: [''],
    remarks:              [''],
  });

  constructor() {
    effect(() => {
      this.dataSource.data = this.filteredRecords();
      if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
    });
  }

  ngOnInit(): void {
    this.loadOneBookConfig();
    this.load();
  }

  private loadOneBookConfig(): void {
    this.settingsService.getByCategory('INTEGRATION').subscribe({
      next: (configs) => {
        const get = (key: string) => configs.find(c => c.configKey === key)?.configValue ?? '';
        this.oneBookEnabled.set(get('onebook.enabled') === 'true');
        this.oneBookAllowCash.set(get('onebook.allow_cash_in_cms') !== 'false');
      },
    });
  }

  protected load(): void {
    this.loading.set(true);
    this.explorerService.getAll().subscribe({
      next: (records) => { this.allRecords.set(records); this.loading.set(false); },
      error: ()        => { this.toast.error('Failed to load commission records'); this.loading.set(false); },
    });
  }

  protected clearFilters(): void {
    this.filterSearch.set('');
    this.filterStatus.set('');
    this.filterSource.set('');
    this.filterFrom.set('');
    this.filterTo.set('');
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
      PAYMENT_REQUESTED: 'Requested',
      PARTIAL:           'Partial',
      PAID:              'Paid',
      NOT_APPLICABLE:    'N/A',
      TRANSMITTED:       'Transmitted',
      PROCESSING:        'Processing',
      FAILED:            'Failed',
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

  // ── Request payment ───────────────────────────────────────────────────────────
  protected requestPayment(r: CommissionRecord, event: Event): void {
    event.stopPropagation();
    this.actionLoading.set(true);
    this.explorerService.requestPayment(r.enquiryId).subscribe({
      next: (updated) => {
        this.updateRecord(updated);
        this.toast.success('Payment request submitted');
        this.actionLoading.set(false);
      },
      error: (err) => {
        this.toast.error(this.apiError(err, 'Failed to request payment'));
        this.actionLoading.set(false);
      },
    });
  }

  // ── Approve via OneBook ───────────────────────────────────────────────────────
  protected approveViaOneBook(r: CommissionRecord, event: Event): void {
    event.stopPropagation();
    this.actionLoading.set(true);
    this.explorerService.approvePayout(r.enquiryId).subscribe({
      next: (updated) => {
        this.updateRecord(updated);
        this.toast.success('Payment transmitted to OneBook');
        this.actionLoading.set(false);
      },
      error: (err) => {
        this.toast.error(this.apiError(err, 'Failed to transmit payment to OneBook'));
        this.actionLoading.set(false);
      },
    });
  }

  // ── Record payout ────────────────────────────────────────────────────────────
  protected openPayoutModal(r: CommissionRecord, event: Event, cashOnly = false): void {
    event.stopPropagation();
    this.isCashOnly.set(cashOnly);
    this.payoutForm.reset({
      amount: r.commissionOutstanding > 0 ? r.commissionOutstanding : r.commissionAmount,
      payoutDate: new Date().toISOString().slice(0, 10),
      paymentMode: cashOnly ? 'CASH' : null,
      transactionReference: '',
      remarks: '',
    });
    this.payoutTarget.set(r);
  }

  protected closePayoutModal(): void {
    this.payoutTarget.set(null);
    this.isCashOnly.set(false);
    this.payoutForm.reset();
  }

  protected submitPayout(): void {
    if (this.payoutForm.invalid) { this.payoutForm.markAllAsTouched(); return; }
    const target = this.payoutTarget();
    if (!target) return;

    const v = this.payoutForm.value;
    this.actionLoading.set(true);
    this.explorerService.recordPayout(target.enquiryId, {
      amount:               v.amount,
      payoutDate:           v.payoutDate,
      paymentMode:          v.paymentMode,
      transactionReference: v.transactionReference?.trim() || undefined,
      remarks:              v.remarks?.trim() || undefined,
    }).subscribe({
      next: (updated) => {
        this.updateRecord(updated);
        this.toast.success('Payout recorded');
        this.closePayoutModal();
        this.actionLoading.set(false);
      },
      error: (err) => {
        this.toast.error(this.apiError(err, 'Failed to record payout'));
        this.actionLoading.set(false);
      },
    });
  }

  private updateRecord(updated: CommissionRecord): void {
    this.allRecords.update(records =>
      records.map(r => r.enquiryId === updated.enquiryId ? updated : r));
  }

  private apiError(err: unknown, fallback: string): string {
    if (err instanceof HttpErrorResponse && err.error?.message) return err.error.message;
    return fallback;
  }
}
