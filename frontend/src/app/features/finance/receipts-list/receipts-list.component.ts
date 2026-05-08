import { Component, computed, effect, inject, OnInit, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FinanceService } from '../finance.service';
import { ReceiptSummary } from '../finance.model';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { PaymentModeLabelPipe } from '../../../shared/pipes/payment-mode-label.pipe';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { PAYMENT_MODES } from '../../../shared/utils/payment-mode.utils';
import { printFeeReceipt } from '../../../shared/utils/print-receipt.utils';

@Component({
  selector: 'app-receipts-list',
  standalone: true,
  imports: [
    FormsModule,
    InrPipe, PaymentModeLabelPipe, AppDatePipe,
    PageHeaderComponent, CmsEmptyStateComponent,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatButtonModule, MatTooltipModule, MatProgressSpinnerModule,
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
    'receiptNumber', 'student', 'installmentsCovered',
    'totalAmountPaid', 'paymentMode', 'paymentDate',
    'transactionReference', 'actions',
  ];

  protected readonly dataSource = new MatTableDataSource<ReceiptSummary>([]);
  protected readonly paymentModes = PAYMENT_MODES;

  protected readonly loading      = signal(false);
  protected readonly searchValue  = signal('');
  protected readonly selectedMode = signal('');
  protected readonly dateFrom     = signal('');
  protected readonly dateTo       = signal('');
  private   readonly allReceipts  = signal<ReceiptSummary[]>([]);

  protected readonly filteredReceipts = computed(() => {
    const search = this.searchValue().trim().toLowerCase();
    const mode   = this.selectedMode();
    const from   = this.dateFrom();
    const to     = this.dateTo();

    return this.allReceipts().filter(r => {
      if (search) {
        const hay = [r.receiptNumber, r.studentName, r.rollNumber].join(' ').toLowerCase();
        if (!hay.includes(search)) return false;
      }
      if (mode && r.paymentMode !== mode) return false;
      if (from && r.paymentDate < from)   return false;
      if (to   && r.paymentDate > to)     return false;
      return true;
    });
  });

  protected readonly totalCount       = computed(() => this.allReceipts().length);
  protected readonly filteredCount    = computed(() => this.filteredReceipts().length);
  protected readonly hasActiveFilters = computed(() =>
    !!this.searchValue() || !!this.selectedMode() || !!this.dateFrom() || !!this.dateTo()
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
    this.dateFrom.set('');
    this.dateTo.set('');
  }

  protected printReceipt(r: ReceiptSummary): void {
    printFeeReceipt({
      receiptNumber: r.receiptNumber,
      studentName:   r.studentName,
      rollNumber:    r.rollNumber,
      amountPaid:    r.totalAmountPaid,
      paymentDate:   r.paymentDate,
      paymentMode:   r.paymentMode,
      transactionReference: r.transactionReference,
      installmentBreakdown: r.installmentBreakdown.map(s => ({
        installmentLabel: s.installmentLabel,
        amountApplied: s.amountApplied,
      })),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.financeService.getAllReceiptSummaries().subscribe({
      next: (data) => { this.allReceipts.set(data); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load receipts'); this.loading.set(false); },
    });
  }
}
