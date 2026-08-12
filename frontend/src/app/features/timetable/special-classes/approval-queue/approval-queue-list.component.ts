import { Component, OnInit, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { Observable } from 'rxjs';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { CmsEmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../../shared/row-action-button/row-action-button.component';
import { CmsFlyoutPanelComponent } from '../../../../shared/flyout-panel/flyout-panel.component';
import { ConfirmDialogComponent } from '../../../../shared/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../../core/toast/toast.service';
import { SpecialClassService } from '../special-class.service';
import { SpecialClassOccurrence } from '../special-class.model';

/** BR-55 — admin-facing approval queue for pending special-class/day-repeat requests, mirroring
 *  the fee-refund-list approve/reject shape but via the shared cms-flyout-panel for the reject
 *  reason instead of a bespoke side panel. A DAY_REPEAT row's approve/reject acts on its whole
 *  request_batch_id, not just that one row. */
@Component({
  selector: 'app-approval-queue-list',
  standalone: true,
  imports: [
    FormsModule, DatePipe, MatTableModule, MatSortModule, MatPaginatorModule, MatProgressSpinnerModule, MatDialogModule,
    CmsEmptyStateComponent, CmsRowActionButtonComponent, CmsFlyoutPanelComponent,
  ],
  templateUrl: './approval-queue-list.component.html',
  styleUrl: './approval-queue-list.component.scss',
})
export class ApprovalQueueListComponent implements OnInit {
  private readonly specialClassService = inject(SpecialClassService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatTable) private _matTable?: MatTable<unknown>;
  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns = ['requestedAt', 'occurrenceDate', 'subjectName', 'periodName', 'venueName', 'requestedFacultyName', 'requestedByFacultyName', 'actions'];
  protected readonly dataSource = new MatTableDataSource<SpecialClassOccurrence>([]);
  protected readonly loading = signal(false);
  protected readonly acting = signal(false);

  protected readonly rejectTarget = signal<SpecialClassOccurrence | null>(null);
  protected rejectionReason = '';

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.specialClassService.approvalQueue().subscribe({
      next: (data) => { this.dataSource.data = data; this.loading.set(false); },
      error: () => { this.toast.error('Failed to load the approval queue'); this.loading.set(false); },
    });
  }

  protected confirmApprove(row: SpecialClassOccurrence): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Approve Special Class',
        message: row.requestBatchId
          ? `Approve every session in this day-repeat batch (starting with ${row.subjectName} on ${row.occurrenceDate})?`
          : `Approve the special class for ${row.subjectName} on ${row.occurrenceDate}?`,
        confirmText: 'Approve',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.approve(row);
    });
  }

  private approve(row: SpecialClassOccurrence): void {
    this.acting.set(true);
    const request$: Observable<unknown> = row.requestBatchId
      ? this.specialClassService.approveBatch(row.requestBatchId)
      : this.specialClassService.approve(row.id);
    request$.subscribe({
      next: () => { this.toast.success('Approved'); this.acting.set(false); this.load(); },
      error: (err: any) => { this.toast.error(err?.error?.message ?? 'Failed to approve'); this.acting.set(false); },
    });
  }

  protected startReject(row: SpecialClassOccurrence): void {
    this.rejectionReason = '';
    this.rejectTarget.set(row);
  }

  protected closeRejectPanel(): void {
    this.rejectTarget.set(null);
  }

  protected confirmReject(): void {
    const row = this.rejectTarget();
    if (!row || !this.rejectionReason.trim()) return;
    this.acting.set(true);
    const request$: Observable<unknown> = row.requestBatchId
      ? this.specialClassService.rejectBatch(row.requestBatchId, this.rejectionReason)
      : this.specialClassService.reject(row.id, this.rejectionReason);
    request$.subscribe({
      next: () => { this.toast.success('Rejected'); this.acting.set(false); this.rejectTarget.set(null); this.load(); },
      error: (err: any) => { this.toast.error(err?.error?.message ?? 'Failed to reject'); this.acting.set(false); },
    });
  }
}
