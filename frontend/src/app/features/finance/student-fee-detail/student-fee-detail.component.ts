import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DecimalPipe } from '@angular/common';
import { FinanceService } from '../finance.service';
import { StudentFeeAllocation, SemesterFeeDetail, Receipt } from '../finance.model';
import { CollectPaymentDialogComponent } from '../collect-payment-dialog/collect-payment-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';

export interface ReceiptGroup {
  receiptNumber: string;
  paymentDate: string;
  paymentMode: string;
  transactionReference: string;
  totalAmount: number;
  lines: Receipt[];
}

@Component({
  selector: 'app-student-fee-detail',
  standalone: true,
  imports: [
    DecimalPipe, RouterLink, MatTableModule, MatPaginatorModule, MatSortModule,
    MatIconModule, MatProgressSpinnerModule, MatButtonModule,
    MatDialogModule, MatTooltipModule,
    PageHeaderComponent, CmsStatusBadgeComponent,
  ],
  templateUrl: './student-fee-detail.component.html',
  styleUrl: './student-fee-detail.component.scss',
})
export class StudentFeeDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly financeService = inject(FinanceService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) set paginator(v: MatPaginator) {
    if (v) this.receiptDataSource.paginator = v;
  }
  @ViewChild(MatSort) set sort(v: MatSort) {
    if (v) this.receiptDataSource.sort = v;
  }

  protected readonly semesterLoading = signal(true);
  protected readonly allocation = signal<StudentFeeAllocation | null>(null);
  protected readonly receiptGroups = signal<ReceiptGroup[]>([]);
  protected readonly receiptDataSource = new MatTableDataSource<Receipt>([]);
  protected readonly receiptColumns = [
    'receiptNumber', 'semesterLabel', 'amountPaid', 'paymentDate', 'paymentMode', 'transactionReference',
  ];

  /** Total fee amount across all semesters. */
  protected readonly totalFee = computed(() =>
    this.allocation()?.semesterFees.reduce((s, sf) => s + sf.amount, 0) ?? 0
  );
  /** Total outstanding across all semesters. */
  protected readonly totalOutstanding = computed(() =>
    this.allocation()?.semesterFees.reduce((s, sf) => s + sf.pendingAmount, 0) ?? 0
  );
  /** Total paid across all semesters. */
  protected readonly totalPaid = computed(() =>
    this.allocation()?.semesterFees.reduce((s, sf) => s + sf.amountPaid, 0) ?? 0
  );

  private studentId!: number;

  ngOnInit(): void {
    this.studentId = Number(this.route.snapshot.paramMap.get('studentId'));
    this.loadAll();
  }

  protected isOverdue(sem: SemesterFeeDetail): boolean {
    return sem.pendingAmount > 0 && new Date(sem.dueDate) < new Date();
  }

  protected openCollectPaymentDialog(): void {
    const ref = this.dialog.open(CollectPaymentDialogComponent, {
      width: '520px',
      data: { studentId: this.studentId },
    });
    ref.afterClosed().subscribe((result) => {
      if (result) {
        const breakdown = result.semesterBreakdown
          ?.map((s: any) => `${s.semesterLabel}: ₹${s.amountApplied.toLocaleString('en-IN')}`)
          .join(', ') ?? result.allocationSummary;
        this.toast.success(`Receipt ${result.receiptNumber} — ${breakdown}`);
        this.loadAll();
      }
    });
  }

  private loadAll(): void {
    this.semesterLoading.set(true);

    this.financeService.getSemesterStatus(this.studentId).subscribe({
      next: (data) => {
        this.allocation.set(data);
        this.semesterLoading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load fee details');
        this.semesterLoading.set(false);
      },
    });

    this.financeService.getReceipts(this.studentId).subscribe({
      next: (data) => {
        this.receiptDataSource.data = data;
        this.receiptGroups.set(this.groupReceipts(data));
      },
    });
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
          receiptNumber: r.receiptNumber,
          paymentDate: r.paymentDate,
          paymentMode: r.paymentMode,
          transactionReference: r.transactionReference,
          totalAmount: r.amountPaid,
          lines: [r],
        });
      }
    }
    return Array.from(map.values());
  }
}
