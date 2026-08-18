import { Component, OnInit, ViewChild, inject, signal } from '@angular/core';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { CmsStatusBadgeComponent } from '../../../../shared/status-badge/status-badge.component';
import { CmsEmptyStateComponent } from '../../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../../shared/row-action-button/row-action-button.component';
import { ConfirmDialogComponent } from '../../../../shared/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../../core/toast/toast.service';
import { SpecialClassService } from '../special-class.service';
import { SpecialClassOccurrence } from '../special-class.model';
import { SpecialClassRequestFlyoutComponent } from '../special-class-request-flyout/special-class-request-flyout.component';
import { TourService } from '../../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../../shared/tour/tour-button.component';
import { MY_SPECIAL_CLASSES_TOUR, MY_SPECIAL_CLASSES_FLOW_MAP } from '../../../../shared/tour/tours/special-class.tours';

/** BR-55 — a faculty member's own special-class/day-repeat requests, mirroring the
 *  my-timetable vs timetable-view split idiom (own-scoped screen, separate from the admin-facing
 *  Approval Queue). */
@Component({
  selector: 'app-my-requests-list',
  standalone: true,
  imports: [
    MatTableModule, MatSortModule, MatPaginatorModule, MatProgressSpinnerModule, MatDialogModule,
    CmsStatusBadgeComponent, CmsEmptyStateComponent, CmsRowActionButtonComponent, SpecialClassRequestFlyoutComponent, CmsTourButtonComponent,
  ],
  templateUrl: './my-requests-list.component.html',
  styleUrl: './my-requests-list.component.scss',
})
export class MyRequestsListComponent implements OnInit {
  private readonly specialClassService = inject(SpecialClassService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  @ViewChild(MatTable) private _matTable?: MatTable<unknown>;
  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns = ['occurrenceDate', 'subjectName', 'periodName', 'venueName', 'requestedFacultyName', 'approvalStatus', 'actions'];
  protected readonly dataSource = new MatTableDataSource<SpecialClassOccurrence>([]);
  protected readonly loading = signal(false);
  protected readonly showRequestFlyout = signal(false);

  ngOnInit(): void {
    this.tourService.register('my-special-classes', MY_SPECIAL_CLASSES_TOUR);
    this.tourService.registerFlowMap('my-special-classes', MY_SPECIAL_CLASSES_FLOW_MAP);
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.specialClassService.myRequests().subscribe({
      next: (data) => { this.dataSource.data = data; this.loading.set(false); },
      error: () => { this.toast.error('Failed to load your special class requests'); this.loading.set(false); },
    });
  }

  protected openRequestFlyout(): void {
    this.showRequestFlyout.set(true);
  }

  protected onFlyoutClosed(): void {
    this.showRequestFlyout.set(false);
  }

  protected onFlyoutSaved(): void {
    this.showRequestFlyout.set(false);
    this.load();
  }

  protected canCancel(row: SpecialClassOccurrence): boolean {
    return row.approvalStatus === 'APPROVED' && new Date(row.occurrenceDate) > new Date();
  }

  protected confirmCancel(row: SpecialClassOccurrence): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Cancel Special Class',
        message: `Cancel the special class for ${row.subjectName} on ${row.occurrenceDate}?`,
        confirmText: 'Cancel Class',
        cancelText: 'Keep',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.cancel(row);
    });
  }

  private cancel(row: SpecialClassOccurrence): void {
    this.specialClassService.cancel(row.id).subscribe({
      next: () => { this.toast.success('Special class cancelled'); this.load(); },
      error: (err) => this.toast.error(err?.error?.message ?? 'Failed to cancel special class'),
    });
  }
}
