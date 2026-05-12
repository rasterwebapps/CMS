import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';
import { ScholarshipApplication } from '../scholarship.model';
import { ScholarshipService } from '../scholarship.service';
import { ScholarshipApproveDialogComponent } from '../approve-dialog/scholarship-approve-dialog.component';
import { ScholarshipRejectDialogComponent } from '../reject-dialog/scholarship-reject-dialog.component';

@Component({
  selector: 'app-scholarship-applications-list',
  standalone: true,
  imports: [MatIconModule, MatProgressSpinnerModule, MatDialogModule, MatTableModule, MatSortModule, AppDatePipe, InrPipe,
            CmsEmptyStateComponent, CmsStatusBadgeComponent],
  templateUrl: './scholarship-applications-list.component.html',
  styleUrl: './scholarship-applications-list.component.scss',
})
export class ScholarshipApplicationsListComponent implements OnInit {
  private readonly scholarshipService = inject(ScholarshipService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  protected readonly loading = signal(false);
  protected readonly applications = signal<ScholarshipApplication[]>([]);
  protected readonly displayedColumns = ['studentName', 'scholarshipName', 'academicYearName', 'applicationDate', 'status', 'approvedAmount', 'actions'];
  protected readonly sortState = signal<Sort>({ active: '', direction: '' });
  protected readonly sortedApplications = computed(() => this.sortRows(this.applications(), this.sortState()));

  ngOnInit(): void { this.load(); }

  protected onSort(sort: Sort): void { this.sortState.set(sort); }

  protected approve(row: ScholarshipApplication): void {
    const ref = this.dialog.open(ScholarshipApproveDialogComponent, {
      width: '520px',
      maxWidth: '95vw',
      data: { application: row },
    });
    ref.afterClosed().subscribe((updated: ScholarshipApplication | undefined) => {
      if (updated) this.load();
    });
  }

  protected reject(row: ScholarshipApplication): void {
    const ref = this.dialog.open(ScholarshipRejectDialogComponent, {
      width: '480px',
      maxWidth: '95vw',
      data: { application: row },
    });
    ref.afterClosed().subscribe((updated: ScholarshipApplication | undefined) => {
      if (updated) this.load();
    });
  }

  private sortRows(rows: ScholarshipApplication[], sort: Sort): ScholarshipApplication[] {
    if (!sort.active || !sort.direction) return rows;
    const factor = sort.direction === 'asc' ? 1 : -1;
    return [...rows].sort((a, b) => {
      const av = this.sortValue(a, sort.active);
      const bv = this.sortValue(b, sort.active);
      if (av === bv) return 0;
      if (av == null) return 1;
      if (bv == null) return -1;
      return av < bv ? -1 * factor : factor;
    });
  }

  private sortValue(row: ScholarshipApplication, column: string): string | number | null | undefined {
    if (column === 'approvedAmount') return row.approvedAmount ?? 0;
    return String((row as unknown as Record<string, unknown>)[column] ?? '').toLowerCase();
  }

  private load(): void {
    this.loading.set(true);
    this.scholarshipService.getPendingApplications().subscribe({
      next: data => { this.applications.set(data); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load applications'); this.loading.set(false); },
    });
  }
}



