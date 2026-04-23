import { Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DecimalPipe } from '@angular/common';
import { FinanceService } from '../finance.service';
import {
  StudentFeeAllocation, SemesterFeeDetail, PenaltyResponse, Receipt,
} from '../finance.model';
import { CollectPaymentDialogComponent } from '../collect-payment-dialog/collect-payment-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-student-fee-detail',
  standalone: true,
  imports: [
    DecimalPipe, RouterLink, MatTableModule, MatPaginatorModule, MatSortModule,
    MatIconModule, MatProgressSpinnerModule, MatButtonModule,
    MatDialogModule, MatTooltipModule,
    PageHeaderComponent, CmsStatusBadgeComponent],
  templateUrl: './student-fee-detail.component.html',
  styleUrl: './student-fee-detail.component.scss',
})
export class StudentFeeDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly financeService = inject(FinanceService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.receiptDataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.receiptDataSource.sort = value;
  }

  protected readonly loading = signal(false);
  protected readonly allocation = signal<StudentFeeAllocation | null>(null);
  protected readonly penalties = signal<PenaltyResponse | null>(null);
  protected readonly receiptColumns = ['receiptNumber', 'semesterLabel', 'amountPaid', 'paymentDate', 'paymentMode', 'transactionReference'];
  protected readonly receiptDataSource = new MatTableDataSource<Receipt>([]);

  private studentId!: number;

  ngOnInit(): void {
    this.studentId = Number(this.route.snapshot.paramMap.get('studentId'));
    this.loadAll();
  }

  protected openCollectPaymentDialog(): void {
    const dialogRef = this.dialog.open(CollectPaymentDialogComponent, {
      width: '480px',
      data: { studentId: this.studentId },
    });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.toast.success(`Payment collected. Receipt: ${result.receiptNumber}`);
        this.loadAll();
      }
    });
  }

  protected getStatusClass(status: string): string {
    switch (status?.toUpperCase()) {
      case 'PAID': return 'status-paid';
      case 'PARTIALLY_PAID': return 'status-partial';
      case 'OVERDUE': return 'status-overdue';
      default: return 'status-pending';
    }
  }

  private loadAll(): void {
    this.loading.set(true);
    this.financeService.getSemesterBreakdown(this.studentId).subscribe({
      next: (data) => {
        this.allocation.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load fee details');
        this.loading.set(false);
      },
    });
    this.financeService.getPenalties(this.studentId).subscribe({
      next: (data) => this.penalties.set(data),
    });
    this.financeService.getReceipts(this.studentId).subscribe({
      next: (data) => {
        this.receiptDataSource.data = data;
      },
    });
  }
}
