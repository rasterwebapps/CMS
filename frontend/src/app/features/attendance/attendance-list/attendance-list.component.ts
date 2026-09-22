import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { AttendanceService } from '../attendance.service';
import { Attendance } from '../attendance.model';
import { SubjectService } from '../../subject/subject.service';
import { Subject } from '../../subject/subject.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsIconDeleteComponent } from '../../../shared/icons';
import { ColumnPickerState, CmsColumnPickerComponent } from '../../../shared/column-picker';

import { ColumnResizeDirective, CmsWrapTextToggleComponent } from '../../../shared/column-resize';
import { CmsInfiniteSelectComponent } from '../../../shared/infinite-select/infinite-select.component';
import { InfiniteSelectValue } from '../../../shared/infinite-select/infinite-select.model';
import { staticOptionsFetchPage } from '../../../shared/infinite-select/infinite-select.utils';
import { TourService } from '../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { ATTENDANCE_TOUR, ATTENDANCE_FLOW_MAP } from '../../../shared/tour/tours/attendance.tours';

@Component({
  selector: 'app-attendance-list',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
    CmsRowActionButtonComponent,
    CmsTypeBadgeComponent,
    CmsStatusBadgeComponent,
    CmsIconDeleteComponent,
    CmsColumnPickerComponent, ColumnResizeDirective, CmsWrapTextToggleComponent, CmsTourButtonComponent,
    CmsInfiniteSelectComponent,
],
  templateUrl: './attendance-list.component.html',
  styleUrl: './attendance-list.component.scss',
})
export class AttendanceListComponent implements OnInit {
  private readonly attendanceService = inject(AttendanceService);
  private readonly subjectService = inject(SubjectService);
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

  protected readonly colState = new ColumnPickerState({
    storageKey: 'attendance-list-cols',
    columns: [
      { key: 'date', label: 'Date', mandatory: true },
      { key: 'studentName', label: 'Student' },
      { key: 'subjectName', label: 'Subject' },
      { key: 'type', label: 'Type' },
      { key: 'status', label: 'Status' },
      { key: 'actions', label: 'Actions', mandatory: true, pinnable: false },
    ],
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());
  protected readonly dataSource = new MatTableDataSource<Attendance>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly filterStatus = signal('');
  protected readonly filterDate = signal('');
  /** Records as returned by the backend for the selected subject/date, before the
   *  client-side status filter is applied on top (the backend has no status query param). */
  protected readonly rawRecords = signal<Attendance[]>([]);

  protected readonly statusOptions = ['PRESENT', 'ABSENT', 'LATE', 'EXCUSED'];
  protected readonly subjectFetchPage = staticOptionsFetchPage(() =>
    this.subjects().map(s => ({ id: s.id, name: s.name })));
  protected readonly statusFetchPage = staticOptionsFetchPage(() =>
    this.statusOptions.map(s => ({ id: s, name: s })));

  /** The backend requires at least one filter on GET /attendance (subjectId, studentId, or
   *  both) -- there is no unfiltered "list everything" endpoint, matching this page's own
   *  "by course and date" subtitle. Subject selection is mandatory before any record loads. */
  protected readonly subjects = signal<Subject[]>([]);
  protected readonly selectedSubjectId = signal<number | null>(null);

  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }

  ngOnInit(): void {
    this.tourService.register('attendance-list', ATTENDANCE_TOUR);
    this.tourService.registerFlowMap('attendance-list', ATTENDANCE_FLOW_MAP);
    this.loadSubjects();
  }

  protected onSubjectChange(value: InfiniteSelectValue | null): void {
    const subjectId = value != null ? Number(value) : null;
    this.selectedSubjectId.set(subjectId);
    if (subjectId) {
      this.loadAttendance(subjectId);
    } else {
      this.rawRecords.set([]);
      this.dataSource.data = [];
    }
  }

  protected onDateChange(value: string): void {
    this.filterDate.set(value);
    const subjectId = this.selectedSubjectId();
    if (subjectId) {
      this.loadAttendance(subjectId);
    }
  }

  protected onStatusFilterChange(value: InfiniteSelectValue | null): void {
    this.filterStatus.set(value != null ? String(value) : '');
    this.applyStatusFilter();
  }

  protected applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.searchValue.set(filterValue);
    this.dataSource.filter = filterValue.trim().toLowerCase();
    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.dataSource.filter = '';
  }

  protected deleteAttendance(attendance: Attendance): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Attendance Record',
        message: `Delete attendance record for ${attendance.studentName} on ${attendance.date}?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.attendanceService.delete(attendance.id).subscribe({
          next: () => {
            this.toast.success('Attendance record deleted');
            const subjectId = this.selectedSubjectId();
            if (subjectId) this.loadAttendance(subjectId);
          },
          error: (err) => {
            this.toast.error(err?.error?.message ?? 'Failed to delete record');
          },
        });
      }
    });
  }

  private loadSubjects(): void {
    this.subjectService.getAll().subscribe({
      next: (data) => this.subjects.set(data),
      error: () => this.toast.error('Failed to load subjects'),
    });
  }

  private loadAttendance(subjectId: number): void {
    this.loading.set(true);
    this.attendanceService.getBySubject(subjectId, this.filterDate() || undefined).subscribe({
      next: (records) => {
        this.rawRecords.set(records);
        this.applyStatusFilter();
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load attendance');
        this.loading.set(false);
      },
    });
  }

  private applyStatusFilter(): void {
    const status = this.filterStatus();
    const rows = this.rawRecords();
    this.dataSource.data = status ? rows.filter((r) => r.status === status) : rows;
  }
}
