import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AcademicYearService } from '../academic-year.service';
import { AcademicYear, Semester } from '../academic-year.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';

@Component({
  selector: 'app-semester-list',
  standalone: true,
  imports: [
    CmsEmptyStateComponent,
    RouterLink,
    DatePipe,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
  ],
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

  private readonly VIEW_MODE_KEY = 'semester-list-view-mode';

  private readonly allSemesters = signal<Semester[]>([]);
  protected readonly dataSource = new MatTableDataSource<Semester>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());
  protected readonly academicYears = signal<AcademicYear[]>([]);
  protected readonly selectedAcademicYearId = signal<number | null>(null);

  protected readonly displayedColumns: readonly string[] = ['name', 'semesterNumber', 'startDate', 'endDate', 'academicYear', 'actions'];

  protected readonly filteredSemesters = computed(() => {
    const q = this.searchValue().toLowerCase().trim();
    if (!q) return this.allSemesters();
    return this.allSemesters().filter(s =>
      s.name.toLowerCase().includes(q) ||
      String(s.semesterNumber).includes(q) ||
      (s.academicYear?.name ?? '').toLowerCase().includes(q)
    );
  });

  protected readonly totalCount = computed(() => this.allSemesters().length);

  ngOnInit(): void {
    this.loadAcademicYears();
    this.loadSemesters();
  }

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
    localStorage.setItem(this.VIEW_MODE_KEY, mode);
  }

  private loadViewMode(): 'card' | 'table' {
    const stored = localStorage.getItem(this.VIEW_MODE_KEY);
    return stored === 'table' ? 'table' : 'card';
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
        this.allSemesters.set(semesters);
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
