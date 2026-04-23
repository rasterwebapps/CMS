import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
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
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-attendance-list',
  standalone: true,
  imports: [
    PageHeaderComponent,
    RouterLink,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatTooltipModule],
  templateUrl: './attendance-list.component.html',
  styleUrl: './attendance-list.component.scss',
})
export class AttendanceListComponent implements OnInit {
  private readonly attendanceService = inject(AttendanceService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  // ── Column visibility ────────────────────────────────────────────────────
  protected readonly ALL_COLS = ['date', 'studentName', 'courseName', 'type', 'status', 'actions'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    date: 'Date',
    studentName: 'Student',
    courseName: 'Course',
    type: 'Type',
    status: 'Status',
    actions: 'Actions',
  };
  private readonly COLS_KEY = 'attendance-list-cols';
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));
  protected readonly dataSource = new MatTableDataSource<Attendance>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly filterStatus = signal('');
  protected readonly filterDate = signal('');

  protected readonly statusOptions = ['PRESENT', 'ABSENT', 'LATE', 'EXCUSED'];

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
          error: () => {
            this.toast.error('Failed to delete record');
          },
        });
      }
    });
  }

  private _loadColPrefs(): Set<string> {
    try {
      const s = localStorage.getItem(this.COLS_KEY);
      if (s) return new Set<string>(JSON.parse(s) as string[]);
    } catch { /* empty */ }
    return new Set<string>(this.ALL_COLS);
  }

  protected toggleColumn(col: string): void {
    this._visibleCols.update(s => {
      const next = new Set(s);
      if (next.size > 1 && next.has(col)) { next.delete(col); } else { next.add(col); }
      localStorage.setItem(this.COLS_KEY, JSON.stringify([...next]));
      return next;
    });
  }

  protected isColumnVisible(col: string): boolean { return this._visibleCols().has(col); }

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
