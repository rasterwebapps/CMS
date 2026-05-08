import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { LowerCasePipe, TitleCasePipe } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AdmissionService } from '../admission.service';
import { AdmissionResponse } from '../admission.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ADMISSION_LIST_TOUR } from '../../../shared/tour/tours/admission.tours';
import { computeInitials } from '../../../shared/utils/initials';

@Component({
  selector: 'app-admission-list',
  standalone: true,
  imports: [
    FormsModule,
    LowerCasePipe,
    TitleCasePipe,
    AppDatePipe,
    CmsEmptyStateComponent,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatTooltipModule,
    CmsTourButtonComponent,
  ],
  templateUrl: './admission-list.component.html',
  styleUrl: './admission-list.component.scss',
})
export class AdmissionListComponent implements OnInit {
  private readonly admissionService = inject(AdmissionService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  @ViewChild(MatPaginator) set paginator(v: MatPaginator) {
    if (v) this.dataSource.paginator = v;
  }
  @ViewChild(MatSort) set sort(v: MatSort) {
    if (v) this.dataSource.sort = v;
  }

  protected readonly computeInitials = computeInitials;

  // ── Column visibility ────────────────────────────────────────
  protected readonly ALL_COLS = [
    'studentName', 'rollNumber', 'program', 'course',
    'semester', 'applicationDate', 'academicYear', 'consent', 'declarationDate', 'studentStatus', 'actions',
  ];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    studentName:     'Student',
    rollNumber:      'Roll No.',
    program:         'Program',
    course:          'Course',
    semester:        'Sem',
    applicationDate: 'Application Date',
    academicYear:    'Joining Year',
    consent:         'Consent',
    declarationDate: 'Declaration Date',
    studentStatus:   'Status',
    actions:         'Actions',
  };
  // Default visible columns (hide rollNumber & course by default to keep table lean)
  private readonly DEFAULT_COLS = new Set([
    'studentName', 'program', 'course', 'semester',
    'applicationDate', 'academicYear', 'consent', 'studentStatus', 'actions',
  ]);
  private readonly COLS_KEY = 'admission-list-cols-v3';
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() =>
    this.ALL_COLS.filter(c => this._visibleCols().has(c)),
  );

  protected readonly dataSource = new MatTableDataSource<AdmissionResponse>([]);
  protected readonly loading = signal(false);

  // ── Filters ──────────────────────────────────────────────────
  protected filterProgram = signal<string>('ALL');
  protected filterStatus  = signal<string>('ALL');
  protected readonly programs = computed(() =>
    [...new Set(this._allData().map(r => r.programName).filter(Boolean))].sort() as string[]
  );
  protected readonly hasActiveFilters = computed(() =>
    this.searchTerm() !== '' || this.filterProgram() !== 'ALL' || this.filterStatus() !== 'ALL'
  );
  protected readonly STUDENT_STATUSES = ['ACTIVE', 'INACTIVE', 'GRADUATED', 'DROPPED'];

  // ── Filters ──────────────────────────────────────────────────
  protected readonly searchTerm = signal('');
  protected colMenuOpen = false;

  // ── Stats ────────────────────────────────────────────────────
  private readonly _allData = signal<AdmissionResponse[]>([]);
  protected readonly totalCount = computed(() => this._allData().length);

  ngOnInit(): void {
    this.tourService.register('admission-list', ADMISSION_LIST_TOUR);
    this.load();
  }

  protected onSearch(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
    this.applyFilters();
  }

  protected clearSearch(): void {
    this.searchTerm.set('');
    this.applyFilters();
  }

  protected onFilterChange(): void { this.applyFilters(); }

  protected clearAllFilters(): void {
    this.searchTerm.set('');
    this.filterProgram.set('ALL');
    this.filterStatus.set('ALL');
    this.applyFilters();
  }

  private _loadColPrefs(): Set<string> {
    try {
      const s = localStorage.getItem(this.COLS_KEY);
      if (s) return new Set<string>(JSON.parse(s) as string[]);
    } catch { /* empty */ }
    return new Set<string>(this.DEFAULT_COLS);
  }

  protected toggleColumn(col: string): void {
    this._visibleCols.update(s => {
      const next = new Set(s);
      if (next.size > 1 && next.has(col)) next.delete(col); else next.add(col);
      localStorage.setItem(this.COLS_KEY, JSON.stringify([...next]));
      return next;
    });
  }

  protected isColumnVisible(col: string): boolean {
    return this._visibleCols().has(col);
  }

  private load(): void {
    this.loading.set(true);
    this.admissionService.getAll().subscribe({
      next: (data) => {
        this._allData.set(data);
        this.dataSource.data = data;
        this.applyFilters();
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load admissions');
        this.loading.set(false);
      },
    });
  }

  private applyFilters(): void {
    const term    = this.searchTerm().toLowerCase().trim();
    const program = this.filterProgram();
    const status  = this.filterStatus();

    this.dataSource.filterPredicate = (row) => {
      if (program !== 'ALL' && (row.programName ?? '') !== program) return false;
      if (status  !== 'ALL' && (row.studentStatus ?? '') !== status) return false;
      if (!term) return true;
      return (
        row.studentName.toLowerCase().includes(term) ||
        (row.rollNumber  ?? '').toLowerCase().includes(term) ||
        (row.programName ?? '').toLowerCase().includes(term) ||
        (row.courseName  ?? '').toLowerCase().includes(term)
      );
    };
    this.dataSource.filter = term || program !== 'ALL' || status !== 'ALL' ? (term || program || status || '_') : '';
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  protected view(item: AdmissionResponse): void {
    void this.router.navigate(['/admissions', item.id]);
  }

  protected edit(item: AdmissionResponse): void {
    void this.router.navigate(['/admissions', item.id, 'edit']);
  }

  protected delete(item: AdmissionResponse): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Admission',
        message: `Delete admission for "${item.studentName}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.admissionService.delete(item.id).subscribe({
          next: () => { this.toast.success('Deleted'); this.load(); },
          error: () => this.toast.error('Failed to delete'),
        });
      }
    });
  }
}
