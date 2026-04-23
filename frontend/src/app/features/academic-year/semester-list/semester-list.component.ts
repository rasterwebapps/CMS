import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AcademicYearService } from '../academic-year.service';
import { AcademicYear, Semester } from '../academic-year.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-semester-list',
  standalone: true,
  imports: [
    PageHeaderComponent,
    RouterLink,
    DatePipe,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatTooltipModule],
  templateUrl: './semester-list.component.html',
  styleUrl: './semester-list.component.scss',
})
export class SemesterListComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  // ── Column visibility ────────────────────────────────────────────────────
  protected readonly ALL_COLS = ['name', 'semesterNumber', 'startDate', 'endDate', 'academicYear', 'actions'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    name: 'Name',
    semesterNumber: 'No.',
    startDate: 'Start Date',
    endDate: 'End Date',
    academicYear: 'Academic Year',
    actions: 'Actions',
  };
  private readonly COLS_KEY = 'semester-list-cols';
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));
  protected readonly dataSource = new MatTableDataSource<Semester>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly selectedAcademicYearId = signal<number | null>(null);

  ngOnInit(): void {
    this.loadAcademicYears();
    this.loadSemesters();
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

  protected onAcademicYearChange(academicYearId: number | null): void {
    this.selectedAcademicYearId.set(academicYearId);
    this.loadSemesters();
  }

  protected editSemester(semester: Semester): void {
    void this.router.navigate(['/semesters', semester.id, 'edit']);
  }

  protected deleteSemester(semester: Semester): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Semester',
        message: `Are you sure you want to delete "${semester.name}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.performDelete(semester);
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

  private performDelete(semester: Semester): void {
    this.loading.set(true);
    this.academicYearService.deleteSemester(semester.id).subscribe({
      next: () => {
        this.toast.success('Semester deleted successfully');
        this.loadSemesters();
      },
      error: () => {
        this.toast.error('Failed to delete semester');
        this.loading.set(false);
      },
    });
  }

  private loadAcademicYears(): void {
    this.academicYearService.getAllAcademicYears().subscribe({
      next: (academicYears) => {
        this.academicYears.set(academicYears);
      },
      error: () => {
        this.toast.error('Failed to load academic years');
      },
    });
  }

  private loadSemesters(): void {
    this.loading.set(true);

    const academicYearId = this.selectedAcademicYearId();
    const request$ = academicYearId
      ? this.academicYearService.getSemestersByAcademicYear(academicYearId)
      : this.academicYearService.getAllSemesters();

    request$.subscribe({
      next: (semesters) => {
        this.dataSource.data = semesters;
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load semesters');
        this.loading.set(false);
      },
    });
  }
}
