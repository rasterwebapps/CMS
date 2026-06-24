import {
  Component, computed, effect, inject, ElementRef, OnInit, signal, ViewChild,
} from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TitleCasePipe } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpErrorResponse } from '@angular/common/http';
import { FinanceService } from '../finance.service';
import { FeeRefundSummary } from '../finance.model';
import { SettingsService } from '../../settings/settings.service';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { PaymentModeLabelPipe } from '../../../shared/pipes/payment-mode-label.pipe';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CashDenominationComponent } from '../../../shared/cash-denomination/cash-denomination.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { TourService } from '../../../shared/tour/tour.service';
import { FEE_REFUND_LIST_TOUR } from '../../../shared/tour/tours/finance.tours';
import { ToastService } from '../../../core/toast/toast.service';
import { PAYMENT_MODES } from '../../../shared/utils/payment-mode.utils';
import { transactionReferenceRequiredValidator } from '../../../shared/validators/transaction-reference-validator';
import { printRefundVoucher, downloadRefundVoucher, RefundVoucherData } from '../../../shared/utils/print-receipt.utils';
import { CmsIconViewComponent } from '../../../shared/icons';

type PanelMode = 'view' | 'approve' | 'reject';

@Component({
  selector: 'app-fee-refund-list',
  standalone: true,
  imports: [
    FormsModule, ReactiveFormsModule, TitleCasePipe,
    InrPipe, PaymentModeLabelPipe, AppDatePipe,
    CmsEmptyStateComponent, CashDenominationComponent, CmsTourButtonComponent,
    CmsRowActionButtonComponent, CmsTypeBadgeComponent,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatTooltipModule, MatProgressSpinnerModule,
      CmsIconViewComponent,
  ],
  templateUrl: './fee-refund-list.component.html',
  styleUrl: './fee-refund-list.component.scss',
})
export class FeeRefundListComponent implements OnInit {
  private readonly financeService  = inject(FinanceService);
  private readonly settingsService = inject(SettingsService);
  private readonly toast           = inject(ToastService);
  private readonly fb              = inject(FormBuilder);
  private readonly tourService     = inject(TourService);

  @ViewChild(MatPaginator) set paginator(v: MatPaginator) { if (v) this.dataSource.paginator = v; }
  @ViewChild(MatSort)      set sort(v: MatSort)           { if (v) this.dataSource.sort = v; }
  @ViewChild('panelBody')  private panelBody!: ElementRef<HTMLElement>;

  protected readonly displayedColumns = [
    'requestedAt', 'originalReceiptNumber', 'student', 'programName',
    'refundAmount', 'requestedBy', 'status', 'actions',
  ];

  protected readonly dataSource    = new MatTableDataSource<FeeRefundSummary>([]);
  protected readonly paymentModes  = PAYMENT_MODES;

  protected readonly oneBookEnabled     = signal(false);
  protected readonly oneBookAllowCash   = signal(true);

  protected readonly loading            = signal(false);
  protected readonly submittingOneBook  = signal(false);
  protected readonly searchValue        = signal('');
  protected readonly statusFilter       = signal<'' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'TRANSMITTED' | 'PAYMENT_FAILED'>('');
  protected readonly filterEntityType   = signal<'' | 'STUDENT' | 'ENQUIRY'>('');
  protected readonly filterProgram      = signal('');
  protected readonly filterAcademicYearId = signal<number | null>(null);
  protected readonly dateFrom           = signal('');
  protected readonly dateTo             = signal('');
  private   readonly allRefunds         = signal<FeeRefundSummary[]>([]);

  protected readonly programs = computed(() =>
    [...new Set(this.allRefunds().map(r => r.programName).filter(Boolean))].sort() as string[]
  );

  protected readonly academicYears = computed(() => {
    const seen = new Map<number, string>();
    for (const r of this.allRefunds()) {
      if (r.academicYearId && r.academicYearName && !seen.has(r.academicYearId)) {
        seen.set(r.academicYearId, r.academicYearName);
      }
    }
    return [...seen.entries()].map(([id, name]) => ({ id, name })).sort((a, b) => b.name.localeCompare(a.name));
  });

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

  protected readonly filteredRefunds = computed(() => {
    const search     = this.searchValue().trim().toLowerCase();
    const status     = this.statusFilter();
    const entityType = this.filterEntityType();
    const program    = this.filterProgram();
    const academicYearId = this.filterAcademicYearId();
    const from       = this.dateFrom();
    const to         = this.dateTo();

    return this.allRefunds().filter(r => {
      if (status     && r.status !== status)         return false;
      if (entityType && r.entityType !== entityType) return false;
      if (program    && r.programName !== program)   return false;
      if (academicYearId && r.academicYearId !== academicYearId) return false;
      if (search) {
        const hay = [
          r.studentName, r.admissionNumber ?? '', r.rollNumber ?? '',
          r.originalReceiptNumber, r.programName ?? '',
        ].join(' ').toLowerCase();
        if (!hay.includes(search)) return false;
      }
      if (from || to) {
        const date = r.requestedAt.substring(0, 10);
        if (from && date < from) return false;
        if (to   && date > to)   return false;
      }
      return true;
    });
  });

  protected readonly totalCount       = computed(() => this.allRefunds().length);
  protected readonly filteredCount    = computed(() => this.filteredRefunds().length);
  protected readonly pendingCount     = computed(() => this.allRefunds().filter(r => r.status === 'PENDING').length);
  protected readonly hasActiveFilters = computed(() =>
    !!this.searchValue() || !!this.statusFilter() || !!this.filterEntityType() ||
    !!this.filterProgram() || !!this.filterAcademicYearId() || !!this.dateFrom() || !!this.dateTo()
  );

  constructor() {
    effect(() => {
      this.dataSource.data = this.filteredRefunds();
      if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
    });
    this.approvalForm.get('paymentMode')?.valueChanges.subscribe(() => {
      this.approvalForm.get('transactionReference')?.updateValueAndValidity();
      this.denominationValid.set(false);
    });
  }

  ngOnInit(): void {
    this.tourService.register('fee-refund-list', FEE_REFUND_LIST_TOUR);
    this.load();
    this.loadOneBookConfig();
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

  private load(): void {
    this.loading.set(true);
    this.financeService.getAllRefunds().subscribe({
      next: data => { this.allRefunds.set(data); this.loading.set(false); },
      error: ()   => { this.loading.set(false); this.toast.error('Failed to load refund requests.'); },
    });
  }

  protected clearFilters(): void {
    this.searchValue.set('');
    this.statusFilter.set('');
    this.filterEntityType.set('');
    this.filterProgram.set('');
    this.filterAcademicYearId.set(null);
    this.dateFrom.set('');
    this.dateTo.set('');
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
        this.allRefunds.update(list => list.map(r => r.id === target.id ? updated : r));
        this.selectedRefund.set(updated);
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
        this.allRefunds.update(list =>
          list.map(r => r.id === updated.id ? updated : r)
            .sort((a, b) => {
              if (a.status === 'PENDING' && b.status !== 'PENDING') return -1;
              if (a.status !== 'PENDING' && b.status === 'PENDING') return 1;
              return b.requestedAt.localeCompare(a.requestedAt);
            })
        );
        this.selectedRefund.set(updated);
        this.panelMode.set('view');
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
        this.allRefunds.update(list =>
          list.map(r => r.id === updated.id ? updated : r)
        );
        this.selectedRefund.set(updated);
        this.panelMode.set('view');
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
