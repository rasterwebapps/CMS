import { Component, computed, DestroyRef, inject, OnInit, signal, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { debounceTime, distinctUntilChanged, Subject, takeUntil } from 'rxjs';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
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
import { ExportButtonComponent, ExportFormat } from '../../../shared/export-button';
import { PermissionService } from '../../../core/permissions/permission.service';

const DEFAULT_PAGE_SIZE = 25;
const SEARCH_MIN_LENGTH = 2;

@Component({
  selector: 'app-fee-explorer',
  standalone: true,
  imports: [
    InrPipe, MatTableModule, MatPaginatorModule, MatSortModule,
    MatTooltipModule, CmsEmptyStateComponent, CmsStatusBadgeComponent, CmsTourButtonComponent,
    CmsRowActionButtonComponent, CmsIconViewComponent, ExportButtonComponent,
  ],
  templateUrl: './fee-explorer.component.html',
  styleUrl: './fee-explorer.component.scss',
})
export class FeeExplorerComponent implements OnInit {
  private readonly financeService    = inject(FinanceService);
  private readonly router            = inject(Router);
  private readonly route             = inject(ActivatedRoute);
  private readonly toast             = inject(ToastService);
  private readonly tourService       = inject(TourService);
  private readonly permissionService = inject(PermissionService);

  // Server-side paginator: NOT wired to dataSource
  @ViewChild(MatPaginator) paginator?: MatPaginator;
  @ViewChild(MatSort) matSort?: MatSort;

  protected readonly displayedColumns = [
    'rollNumber', 'studentName', 'programName', 'totalFee',
    'totalPaid', 'totalPending', 'totalPenalty', 'allocationStatus', 'actions',
  ];
  protected readonly dataSource    = new MatTableDataSource<StudentFeeSummary>([]);
  protected readonly loading       = signal(false);
  protected readonly exporting     = signal(false);
  protected readonly searchValue   = signal('');
  protected readonly computeInitials = computeInitials;
  protected totalElements          = 0;

  protected readonly canExport = computed(() => this.permissionService.has('STUDENT_FEE_EXPORT'));

  // ── Client-side within-page filters ─────────────────────────────────────
  protected filterProgram      = signal<string>('ALL');
  protected filterAcademicYear = signal<string>('ALL');
  protected filterYearOfStudy  = signal<string>('ALL');
  protected filterAllocStatus  = signal<string>('ALL');

  private readonly _pageData = signal<StudentFeeSummary[]>([]);

  protected readonly programs = computed(() =>
    [...new Set(this._pageData().map(r => r.programName).filter(Boolean))].sort() as string[]
  );
  protected readonly academicYears = computed(() =>
    [...new Set(this._pageData().map(r => r.academicYearName).filter(Boolean))].sort() as string[]
  );
  protected readonly yearsOfStudy = computed(() =>
    [...new Set(this._pageData().map(r => r.yearOfStudy).filter((v): v is number => v != null))]
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

  // ── Pagination / state ────────────────────────────────────────────────────
  private currentPage     = 0;
  private currentPageSize = DEFAULT_PAGE_SIZE;

  private readonly destroy$      = new Subject<void>();
  private readonly searchSubject = new Subject<string>();

  protected onExport(format: ExportFormat): void {
    if (this.exporting()) return;
    this.exporting.set(true);
    this.financeService.exportFeeExplorer(format, {
      search:           this.searchValue() || null,
      program:          this.filterProgram() !== 'ALL' ? this.filterProgram() : null,
      academicYear:     this.filterAcademicYear() !== 'ALL' ? this.filterAcademicYear() : null,
      yearOfStudy:      this.filterYearOfStudy() !== 'ALL' ? Number(this.filterYearOfStudy()) : null,
      allocationStatus: this.filterAllocStatus() !== 'ALL' ? this.filterAllocStatus() : null,
    }).subscribe({
      next: (blob) => {
        const ext = format === 'pdf' ? 'pdf' : 'xlsx';
        const url = URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href     = url;
        a.download = `fee-explorer.${ext}`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting.set(false);
      },
      error: () => {
        this.toast.error('Export failed. Please try again.');
        this.exporting.set(false);
      },
    });
  }

  ngOnInit(): void {
    this.tourService.register('fee-explorer', FEE_EXPLORER_TOUR);

    // URL params drive state — initial load + back-nav restore
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.searchValue.set(params['search'] ?? '');
      this.currentPage     = params['page'] ? +params['page'] : 0;
      this.currentPageSize = params['size'] ? +params['size'] : DEFAULT_PAGE_SIZE;
      this.loadPage();
    });

    // Paginator page events → URL navigate (triggers param subscription above)
    setTimeout(() => {
      this.paginator?.page.pipe(takeUntil(this.destroy$)).subscribe((ev: PageEvent) => {
        this.navigate({ page: ev.pageIndex, size: ev.pageSize });
      });
    }, 0);

    // Debounced search
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(val => {
      if (val.length === 0 || val.length >= SEARCH_MIN_LENGTH) {
        this.navigate({ search: val || null, page: 0 });
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected onSearchInput(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.searchValue.set(val);
    this.searchSubject.next(val);
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.navigate({ search: null, page: 0 });
  }

  protected onFilterChange(): void { this._applyClientFilters(); }

  protected clearAllFilters(): void {
    this.filterProgram.set('ALL');
    this.filterAcademicYear.set('ALL');
    this.filterYearOfStudy.set('ALL');
    this.filterAllocStatus.set('ALL');
    this.searchValue.set('');
    this.navigate({ search: null, page: 0 });
  }

  protected viewDetails(student: StudentFeeSummary): void {
    void this.router.navigate(['/student-fees', student.studentId], {
      queryParams: { returnTo: 'fee-explorer' },
    });
  }

  private navigate(patch: Partial<{ search: string | null; page: number; size: number }>): void {
    const p: Record<string, string | number | null> = {
      search: 'search' in patch ? patch.search ?? null : this.searchValue() || null,
      page:   'page'   in patch ? patch.page!          : this.currentPage,
      size:   'size'   in patch ? patch.size!          : this.currentPageSize,
    };
    // Drop null/default values to keep URL clean
    const qp: Record<string, string | number> = {};
    if (p['search']) qp['search'] = p['search'] as string;
    if ((p['page'] as number) > 0) qp['page'] = p['page'] as number;
    if ((p['size'] as number) !== DEFAULT_PAGE_SIZE) qp['size'] = p['size'] as number;
    void this.router.navigate([], { relativeTo: this.route, queryParams: qp });
  }

  private loadPage(): void {
    this.loading.set(true);
    this.financeService.searchStudentFeesPage({
      search:  this.searchValue() || undefined,
      page:    this.currentPage,
      size:    this.currentPageSize,
    }).subscribe({
      next: (page) => {
        this._pageData.set(page.content);
        this.dataSource.data = page.content;
        this.totalElements   = page.totalElements;
        if (this.paginator) {
          this.paginator.length    = page.totalElements;
          this.paginator.pageIndex = page.number;
          this.paginator.pageSize  = page.size;
        }
        this._applyClientFilters();
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load student fees');
        this.loading.set(false);
      },
    });
  }

  private _applyClientFilters(): void {
    const program = this.filterProgram();
    const ay      = this.filterAcademicYear();
    const yos     = this.filterYearOfStudy();
    const status  = this.filterAllocStatus();

    const anyDropdown = program !== 'ALL' || ay !== 'ALL' || yos !== 'ALL' || status !== 'ALL';
    if (!anyDropdown) {
      this.dataSource.filter = '';
      return;
    }

    this.dataSource.filterPredicate = (row: StudentFeeSummary) => {
      if (program !== 'ALL' && (row.programName ?? '') !== program)   return false;
      if (ay      !== 'ALL' && (row.academicYearName ?? '') !== ay)   return false;
      if (yos     !== 'ALL' && String(row.yearOfStudy ?? '') !== yos) return false;
      if (status  !== 'ALL' && row.allocationStatus !== status)       return false;
      return true;
    };
    this.dataSource.filter = program + ay + yos + status;
  }
}
