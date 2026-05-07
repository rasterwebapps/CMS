import { Component, inject, OnInit, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
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
  imports: [MatIconModule, MatProgressSpinnerModule, MatDialogModule, AppDatePipe, InrPipe,
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

  ngOnInit(): void { this.load(); }

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

  private load(): void {
    this.loading.set(true);
    this.scholarshipService.getPendingApplications().subscribe({
      next: data => { this.applications.set(data); this.loading.set(false); },
      error: () => { this.toast.error('Failed to load applications'); this.loading.set(false); },
    });
  }
}



