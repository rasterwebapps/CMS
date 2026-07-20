import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LabScheduleService } from '../lab-schedule.service';
import { LabSchedule } from '../lab-schedule.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { CmsIconDeleteComponent, CmsIconEditComponent } from '../../../shared/icons';
import { ColumnPickerState, CmsColumnPickerComponent } from '../../../shared/column-picker';

import { ColumnResizeDirective, CmsWrapTextToggleComponent } from '../../../shared/column-resize';

@Component({
  selector: 'app-lab-schedule-list',
  standalone: true,
  imports: [
    CmsTourButtonComponent,
    CmsEmptyStateComponent,
    CmsRowActionButtonComponent,
    CmsTypeBadgeComponent,
    RouterLink, FormsModule, MatTableModule, MatPaginatorModule, MatSortModule,
    MatProgressSpinnerModule, MatDialogModule, MatTooltipModule,
    CmsIconDeleteComponent,
    CmsIconEditComponent,
    CmsColumnPickerComponent, ColumnResizeDirective, CmsWrapTextToggleComponent,
  ],
  templateUrl: './lab-schedule-list.component.html',
  styleUrl: './lab-schedule-list.component.scss',
})
export class LabScheduleListComponent implements OnInit {
  private readonly labScheduleService = inject(LabScheduleService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatTable) private _matTable?: MatTable<unknown>;
  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly colState = new ColumnPickerState({
    storageKey: 'lab-schedule-list-cols',
    columns: [
      { key: 'dayOfWeek', label: 'Day', mandatory: true },
      { key: 'sessionType', label: 'Type' },
      { key: 'roomName', label: 'Room' },
      { key: 'subjectName', label: 'Subject' },
      { key: 'subjectCode', label: 'Code' },
      { key: 'facultyName', label: 'Faculty' },
      { key: 'batchName', label: 'Batch' },
      { key: 'startTime', label: 'Start' },
      { key: 'endTime', label: 'End' },
      { key: 'actions', label: 'Actions', mandatory: true, pinnable: false },
    ],
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());
  protected readonly dataSource = new MatTableDataSource<LabSchedule>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }

  ngOnInit(): void { this.load(); }

  protected applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.dataSource.filter = value.trim().toLowerCase();
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  protected clearFilter(): void { this.searchValue.set(''); this.dataSource.filter = ''; }

  protected edit(item: LabSchedule): void { void this.router.navigate(['/lab-schedules', item.id, 'edit']); }

  protected delete(item: LabSchedule): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete Class Schedule', message: `Delete schedule for "${item.subjectName}" on ${item.dayOfWeek}?`, confirmText: 'Delete', cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => { if (confirmed) this.doDelete(item); });
  }




  private doDelete(item: LabSchedule): void {
    this.loading.set(true);
    this.labScheduleService.delete(item.id).subscribe({
      next: () => { this.toast.success('Deleted successfully'); this.load(); },
      error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to delete'); this.loading.set(false); },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.labScheduleService.getAll().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load'); this.loading.set(false); },
    });
  }
}
