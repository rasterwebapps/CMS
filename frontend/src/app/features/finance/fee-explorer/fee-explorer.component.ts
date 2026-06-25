import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FinanceService } from '../finance.service';
import { StudentFeeSummary } from '../finance.model';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { FEE_EXPLORER_TOUR } from '../../../shared/tour/tours/finance.tours';
import { ToastService } from '../../../core/toast/toast.service';
import { computeInitials } from '../../../shared/utils/initials';
import { CmsIconViewComponent } from '../../../shared/icons';

@Component({
  selector: 'app-fee-explorer',
  standalone: true,
  imports: [
    InrPipe, MatTableModule, MatPaginatorModule, MatSortModule,
    MatTooltipModule, CmsEmptyStateComponent, CmsStatusBadgeComponent, CmsTourButtonComponent,
    CmsRowActionButtonComponent,
      CmsIconViewComponent,
  ],
  templateUrl: './fee-explorer.component.html',
  styleUrl: './fee-explorer.component.scss',
})
export class FeeExplorerComponent implements OnInit {
  private readonly financeService = inject(FinanceService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly tourService = inject(TourService);

  @ViewChild(MatPaginator) set paginator(value: MatPaginator) {
    if (value) this.dataSource.paginator = value;
  }
  @ViewChild(MatSort) set sort(value: MatSort) {
    if (value) this.dataSource.sort = value;
  }

  protected readonly displayedColumns = [
    'rollNumber', 'studentName', 'programName', 'totalFee',
    'totalPaid', 'totalPending', 'totalPenalty', 'allocationStatus', 'actions',
  ];
  protected readonly dataSource = new MatTableDataSource<StudentFeeSummary>([]);
  protected readonly loading    = signal(false);
  protected readonly searchValue = signal('');
  protected readonly computeInitials = computeInitials;

  // ── Filters ──────────────────────────────────────────────────────────────
  protected filterProgram      = signal<string>('ALL');
  protected filterAcademicYear = signal<string>('ALL');
  protected filterYearOfStudy  = signal<string>('ALL');
  protected filterAllocStatus  = signal<string>('ALL');

  private readonly _allData = signal<StudentFeeSummary[]>([]);

  protected readonly programs = computed(() =>
    [...new Set(this._allData().map(r => r.programName).filter(Boolean))].sort() as string[]
  );
  protected readonly academicYears = computed(() =>
    [...new Set(this._allData().map(r => r.academicYearName).filter(Boolean))].sort() as string[]
  );
  protected readonly yearsOfStudy = computed(() =>
    [...new Set(this._allData().map(r => r.yearOfStudy).filter((v): v is number => v != null))]
      .sort((a, b) => a - b)
  );
  protected readonly ALLOC_STATUSES = [
    { value: 'DRAFT',         label: 'Draft' },
    { value: 'FINALIZED',     label: 'Finalized' },
    { value: 'NOT_ALLOCATED', label: 'Not Allocated' },
  ];
  protected readonly hasActiveFilters = computed(() =>
    this.searchValue()        !== '' ||
    this.filterProgram()      !== 'ALL' ||
    this.filterAcademicYear() !== 'ALL' ||
    this.filterYearOfStudy()  !== 'ALL' ||
    this.filterAllocStatus()  !== 'ALL'
  );

  ngOnInit(): void {
    this.tourService.register('fee-explorer', FEE_EXPLORER_TOUR);
    this.load();
  }

  protected applyFilter(event: Event): void {
    this.searchValue.set((event.target as HTMLInputElement).value);
    this._applyFilters();
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this._applyFilters();
  }

  protected onFilterChange(): void { this._applyFilters(); }

  protected clearAllFilters(): void {
    this.searchValue.set('');
    this.filterProgram.set('ALL');
    this.filterAcademicYear.set('ALL');
    this.filterYearOfStudy.set('ALL');
    this.filterAllocStatus.set('ALL');
    this._applyFilters();
  }

  protected searchFromApi(): void {
    this.load(this.searchValue());
  }

  private _applyFilters(): void {
    const term    = this.searchValue().toLowerCase().trim();
    const program = this.filterProgram();
    const ay      = this.filterAcademicYear();
    const yos     = this.filterYearOfStudy();
    const status  = this.filterAllocStatus();

    this.dataSource.filterPredicate = (row: StudentFeeSummary) => {
      if (program !== 'ALL' && (row.programName ?? '') !== program)                return false;
      if (ay      !== 'ALL' && (row.academicYearName ?? '') !== ay)                return false;
      if (yos     !== 'ALL' && String(row.yearOfStudy ?? '') !== yos)              return false;
      if (status  !== 'ALL' && row.allocationStatus !== status)                    return false;
      if (!term) return true;
      return row.studentName.toLowerCase().includes(term) ||
             (row.rollNumber ?? '').toLowerCase().includes(term);
    };
    const any = term || program !== 'ALL' || ay !== 'ALL' || yos !== 'ALL' || status !== 'ALL';
    this.dataSource.filter = any ? (term || program || ay || yos || status || '_') : '';
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  protected viewDetails(student: StudentFeeSummary): void {
    void this.router.navigate(['/student-fees', student.studentId], {
      queryParams: { returnTo: 'fee-explorer' },
    });
  }

  private load(search?: string): void {
    this.loading.set(true);
    this.financeService.searchStudentFees(search).subscribe({
      next: (result) => {
        this._allData.set(result.students);
        this.dataSource.data = result.students;
        this._applyFilters();
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load student fees');
        this.loading.set(false);
      },
    });
  }
}
