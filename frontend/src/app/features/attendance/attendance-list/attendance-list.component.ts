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
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { CmsIconDeleteComponent } from '../../../shared/icons';
import { ColumnPickerState, CmsColumnPickerComponent } from '../../../shared/column-picker';

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
    CmsIconDeleteComponent,
    CmsColumnPickerComponent,
],
  templateUrl: './attendance-list.component.html',
  styleUrl: './attendance-list.component.scss',
})
export class AttendanceListComponent implements OnInit {
  private readonly attendanceService = inject(AttendanceService);
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
    storageKey: 'attendance-list-cols',
    columns: [
      { key: 'date', label: 'Date', mandatory: true },
      { key: 'studentName', label: 'Student' },
      { key: 'courseName', label: 'Course' },
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

  protected readonly statusOptions = ['PRESENT', 'ABSENT', 'LATE', 'EXCUSED'];

  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }

  ngOnInit(): void {
    this.loadAttendance();
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
            this.loadAttendance();
          },
          error: (err) => {
            this.toast.error(err?.error?.message ?? 'Failed to delete record');
          },
        });
      }
    });
  }




  private loadAttendance(): void {
    this.loading.set(true);
    this.attendanceService.getAll().subscribe({
      next: (records) => {
        this.dataSource.data = records;
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load attendance');
        this.loading.set(false);
      },
    });
  }
}
