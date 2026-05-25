import { Component, computed, effect, inject, OnInit, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FinanceService } from '../finance.service';
import { UnifiedReceiptSummary } from '../finance.model';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { PaymentModeLabelPipe } from '../../../shared/pipes/payment-mode-label.pipe';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { PAYMENT_MODES } from '../../../shared/utils/payment-mode.utils';
import { printFeeReceipt, downloadFeeReceipt } from '../../../shared/utils/print-receipt.utils';

@Component({
  selector: 'app-receipts-list',
  standalone: true,
  imports: [
    FormsModule,
    InrPipe, PaymentModeLabelPipe, AppDatePipe,
    CmsEmptyStateComponent,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatTooltipModule, MatProgressSpinnerModule,
  ],
  templateUrl: './receipts-list.component.html',
  styleUrl: './receipts-list.component.scss',
})
export class ReceiptsListComponent implements OnInit {
  private readonly financeService = inject(FinanceService);
  private readonly toast = inject(ToastService);

  @ViewChild(MatPaginator) set paginator(v: MatPaginator) { if (v) this.dataSource.paginator = v; }
  @ViewChild(MatSort)      set sort(v: MatSort)           { if (v) this.dataSource.sort = v; }

  protected readonly displayedColumns = [
    'receiptNumber', 'payer', 'payerType', 'installmentsCovered',
    'amountPaid', 'paymentMode', 'paymentDate',
    'transactionReference', 'actions',
  ];

  protected readonly dataSource = new MatTableDataSource<UnifiedReceiptSummary>([]);
  protected readonly paymentModes = PAYMENT_MODES;

  protected readonly loading      = signal(false);
  protected readonly searchValue  = signal('');
  protected readonly selectedMode = signal('');
  protected readonly selectedType = signal(''); // STUDENT | ENQUIRY | ''
  protected readonly dateFrom     = signal('');
  protected readonly dateTo       = signal('');
  private   readonly allReceipts  = signal<UnifiedReceiptSummary[]>([]);

  protected readonly filteredReceipts = computed(() => {
    const search = this.searchValue().trim().toLowerCase();
    const mode   = this.selectedMode();
    const type   = this.selectedType();
    const from   = this.dateFrom();
    const to     = this.dateTo();

    return this.allReceipts().filter(r => {
      if (search) {
        const hay = [r.receiptNumber, r.payerName, r.payerIdentifier ?? '', r.admissionNumber ?? '']
          .join(' ')
          .toLowerCase();
        if (!hay.includes(search)) return false;
      }
      if (mode && r.paymentMode !== mode) return false;
      if (type && r.payerType !== type)   return false;
      if (from && r.paymentDate < from)   return false;
      if (to   && r.paymentDate > to)     return false;
      return true;
    });
  });

  protected readonly totalCount       = computed(() => this.allReceipts().length);
  protected readonly filteredCount    = computed(() => this.filteredReceipts().length);
  protected readonly totalAmount      = computed(() =>
    this.filteredReceipts().reduce((s, r) => s + r.amountPaid, 0)
  );
  protected readonly hasActiveFilters = computed(() =>
    !!this.searchValue() || !!this.selectedMode() || !!this.selectedType() || !!this.dateFrom() || !!this.dateTo()
  );

  constructor() {
    effect(() => {
      this.dataSource.data = this.filteredReceipts();
      if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
    });
  }

  ngOnInit(): void { this.load(); }

  protected clearFilters(): void {
    this.searchValue.set('');
    this.selectedMode.set('');
    this.selectedType.set('');
    this.dateFrom.set('');
    this.dateTo.set('');
  }

  protected viewReceipt(r: UnifiedReceiptSummary): void {
    void printFeeReceipt({
      receiptNumber:    r.receiptNumber,
      payerName:        r.payerName,
      payerIdentifier:  r.payerIdentifier ?? '',
      admissionNumber:  r.admissionNumber ?? '',
      programName:      r.programName ?? '',
      amountPaid:       r.amountPaid,
      paymentDate:      r.paymentDate,
      paymentMode:      r.paymentMode,
      transactionReference: r.transactionReference,
      feeCategory:      r.feeCategory,
      installmentBreakdown: r.installmentsCovered
        ? [{ installmentLabel: r.installmentsCovered, amountApplied: r.amountPaid }]
        : [],
    });
  }

  protected downloadReceipt(r: UnifiedReceiptSummary): void {
    void downloadFeeReceipt({
      receiptNumber:    r.receiptNumber,
      payerName:        r.payerName,
      payerIdentifier:  r.payerIdentifier ?? '',
      admissionNumber:  r.admissionNumber ?? '',
      programName:      r.programName ?? '',
      amountPaid:       r.amountPaid,
      paymentDate:      r.paymentDate,
      paymentMode:      r.paymentMode,
      transactionReference: r.transactionReference,
      feeCategory:      r.feeCategory,
      installmentBreakdown: r.installmentsCovered
        ? [{ installmentLabel: r.installmentsCovered, amountApplied: r.amountPaid }]
        : [],
    });
  }

  private load(): void {
    this.loading.set(true);
    this.financeService.getUnifiedReceipts().subscribe({
      next:  (data) => { this.allReceipts.set(data); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load receipts'); this.loading.set(false); },
    });
  }
}
