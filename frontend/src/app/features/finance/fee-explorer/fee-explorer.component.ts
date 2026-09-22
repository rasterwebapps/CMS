import { Component, computed, DestroyRef, inject, OnInit, OnDestroy, signal, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { debounceTime, distinctUntilChanged, Subject, Subscription, takeUntil } from 'rxjs';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FinanceService } from '../finance.service';
import { StudentFeeSummary } from '../finance.model';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { FEE_EXPLORER_TOUR, FEE_EXPLORER_FLOW_MAP } from '../../../shared/tour/tours/finance.tours';
import { ToastService } from '../../../core/toast/toast.service';
import { computeInitials } from '../../../shared/utils/initials';
import { CmsIconViewComponent } from '../../../shared/icons';
import { CmsColumnPickerComponent, ColumnPickerState } from '../../../shared/column-picker';
import { ColumnResizeDirective, CmsWrapTextToggleComponent } from '../../../shared/column-resize';
import { CmsInfiniteSelectComponent } from '../../../shared/infinite-select/infinite-select.component';
import { InfiniteSelectValue } from '../../../shared/infinite-select/infinite-select.model';
import { staticOptionsFetchPage } from '../../../shared/infinite-select/infinite-select.utils';
import { ExportFormat } from '../../../shared/export-button';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PermissionService } from '../../../core/permissions/permission.service';

const DEFAULT_PAGE_SIZE = 25;
const SEARCH_MIN_LENGTH = 2;
const DEFAULT_SORT_FIELD = 'rollNumber';
const DEFAULT_SORT_DIR: 'asc' | 'desc' = 'asc';
// Only columns backed by a real, directly-queryable Student entity property (or join
// path) belong here — searchPageable() sorts against Student via Specification before
// the per-row fee totals are computed in Java, so totalFee/totalPaid/totalPending/
// totalPenalty/allocationStatus can never be sorted this way (mat-sort-header removed
// from those columns in the template instead of guessing a mapping that would 500).
const SORT_FIELD_MAP: Record<string, string> = {
  rollNumber:  'rollNumber',
  studentName: 'firstName',
  programName: 'program.name',
};
@Component({
  selector: 'app-fee-explorer',
  standalone: true,
  imports: [
    InrPipe, MatTableModule, MatPaginatorModule, MatSortModule,
    MatTooltipModule, CmsEmptyStateComponent, CmsStatusBadgeComponent, CmsTourButtonComponent,
    CmsRowActionButtonComponent, CmsIconViewComponent,
    CmsColumnPickerComponent, ColumnResizeDirective, CmsWrapTextToggleComponent,
    MatMenuModule, MatButtonModule, MatIconModule,
    CmsInfiniteSelectComponent,
  ],
  templateUrl: './fee-explorer.component.html',
  styleUrl: './fee-explorer.component.scss',
})

export class FeeExplorerComponent implements OnInit, OnDestroy {
  private readonly financeService    = inject(FinanceService);
  private readonly router            = inject(Router);
  private readonly route             = inject(ActivatedRoute);
  private readonly toast             = inject(ToastService);
  private readonly tourService       = inject(TourService);
  private readonly permissionService = inject(PermissionService);

  @ViewChild(MatTable) private _matTable?: MatTable<unknown>;
  @ViewChild(MatPaginator)
  set paginator(value: MatPaginator | undefined) {
    if (this._paginator === value) return;
    this._paginatorSub?.unsubscribe();
    this._paginator = value;
    if (!value) return;
    this._paginatorSub = value.page.pipe(takeUntil(this.destroy$)).subscribe((ev: PageEvent) => {
      this.navigate({ page: ev.pageIndex, size: ev.pageSize });
    });
    this.syncPaginatorState();
  }
  get paginator(): MatPaginator | undefined { return this._paginator; }
  private _paginator?: MatPaginator;
  private _paginatorSub?: Subscription;
  protected readonly colState = new ColumnPickerState({
    columns: [
      { key: 'rollNumber',       label: 'Roll No.' },
      { key: 'studentName',      label: 'Student',   mandatory: true },
      { key: 'programName',      label: 'Program' },
      { key: 'totalFee',         label: 'Total Fee' },
      { key: 'totalPaid',        label: 'Paid' },
      { key: 'totalPending',     label: 'Pending' },
      { key: 'totalPenalty',     label: 'Penalty' },
      { key: 'allocationStatus', label: 'Status' },
      { key: 'actions',          label: 'Actions',   mandatory: true, pinnable: false },
    ],
    storageKey: 'fee-explorer-cols-v1',
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());
  protected readonly dataSource    = new MatTableDataSource<StudentFeeSummary>([]);
  protected readonly loading       = signal(false);
  protected readonly exporting     = signal(false);
  protected readonly searchValue   = signal('');
  protected readonly computeInitials = computeInitials;
  protected totalElements          = 0;

  protected readonly canExport = computed(() => this.permissionService.has('STUDENT_FEE_EXPORT'));
  protected readonly canExportSemWise = computed(() => this.permissionService.has('STUDENT_FEE_EXPORT_SEMESTER_WISE'));

  // ── Server-side filters — sent to the backend and reflected in the URL ───
  protected filterProgram      = signal<string>('ALL');
  protected filterAcademicYear = signal<string>('ALL');
  protected filterYearOfStudy  = signal<string>('ALL');
  protected filterAllocStatus  = signal<string>('ALL');

  // Dropdown option lists span every student, not just whatever page is currently loaded.
  protected readonly programs      = signal<string[]>([]);
  protected readonly academicYears = signal<string[]>([]);
  protected readonly yearsOfStudy  = signal<number[]>([]);

  protected readonly ALLOC_STATUSES = [
    { value: 'DRAFT',         label: 'Draft' },
    { value: 'FINALIZED',     label: 'Finalized' },
    { value: 'NOT_ALLOCATED', label: 'Not Allocated' },
  ];
  protected readonly programFetchPage = staticOptionsFetchPage(() =>
    this.programs().map(p => ({ id: p, name: p })));
  protected readonly academicYearFetchPage = staticOptionsFetchPage(() =>
    this.academicYears().map(ay => ({ id: ay, name: ay })));
  protected readonly yearOfStudyFetchPage = staticOptionsFetchPage(() =>
    this.yearsOfStudy().map(y => ({ id: String(y), name: `Year ${y}` })));
  protected readonly allocStatusFetchPage = staticOptionsFetchPage(() =>
    this.ALLOC_STATUSES.map(s => ({ id: s.value, name: s.label })));
  protected readonly hasActiveFilters = computed(() =>
    this.searchValue()        !== '' ||
    this.filterProgram()      !== 'ALL' ||
    this.filterAcademicYear() !== 'ALL' ||
    this.filterYearOfStudy()  !== 'ALL' ||
    this.filterAllocStatus()  !== 'ALL'
  );

  // ── Pagination / sort state ───────────────────────────────────────────────
  private currentPage      = 0;
  private currentPageSize  = DEFAULT_PAGE_SIZE;
  protected sortActive     = DEFAULT_SORT_FIELD;
  protected sortDirection: 'asc' | 'desc' = DEFAULT_SORT_DIR;

  private readonly destroy$      = new Subject<void>();
  private readonly searchSubject = new Subject<string>();

  protected onExport(format: ExportFormat): void {
    if (this.exporting()) return;
    if (this.totalElements === 0) {
      this.toast.error('No data available to export.');
      return;
    }
    this.exporting.set(true);
    this.financeService.exportFeeExplorer(format, {
      search:           this.searchValue() || null,
      program:          this.filterProgram() !== 'ALL' ? this.filterProgram() : null,
      academicYear:     this.filterAcademicYear() !== 'ALL' ? this.filterAcademicYear() : null,
      yearOfStudy:      this.filterYearOfStudy() !== 'ALL' ? Number(this.filterYearOfStudy()) : null,
      allocationStatus: this.filterAllocStatus() !== 'ALL' ? this.filterAllocStatus() : null,
      sort:             SORT_FIELD_MAP[this.sortActive] ?? this.sortActive,
      direction:        this.sortDirection,
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

  protected onExportSemWise(format: ExportFormat): void {
    if (this.exporting()) return;
    if (this.totalElements === 0) {
      this.toast.error('No data available to export.');
      return;
    }
    this.exporting.set(true);
    this.financeService.exportFeeExplorerSemesterWise(format, {
      search:           this.searchValue() || null,
      program:          this.filterProgram() !== 'ALL' ? this.filterProgram() : null,
      academicYear:     this.filterAcademicYear() !== 'ALL' ? this.filterAcademicYear() : null,
      yearOfStudy:      this.filterYearOfStudy() !== 'ALL' ? Number(this.filterYearOfStudy()) : null,
      allocationStatus: this.filterAllocStatus() !== 'ALL' ? this.filterAllocStatus() : null,
      sort:             SORT_FIELD_MAP[this.sortActive] ?? this.sortActive,
      direction:        this.sortDirection,
    }).subscribe({
      next: (blob) => {
        const ext = format === 'pdf' ? 'pdf' : 'xlsx';
        const url = URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href     = url;
        a.download = `fee-explorer-semester-wise.${ext}`;
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

  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }
  ngOnInit(): void {
    this.tourService.register('fee-explorer', FEE_EXPLORER_TOUR);
    this.tourService.registerFlowMap('fee-explorer', FEE_EXPLORER_FLOW_MAP);
    this.loadFilterOptions();

    // URL params drive state — initial load + back-nav restore
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.searchValue.set(params['search'] ?? '');
      this.filterProgram.set(params['program'] ?? 'ALL');
      this.filterAcademicYear.set(params['academicYear'] ?? 'ALL');
      this.filterYearOfStudy.set(params['yearOfStudy'] ?? 'ALL');
      this.filterAllocStatus.set(params['allocationStatus'] ?? 'ALL');
      this.currentPage      = params['page']      ? +params['page']     : 0;
      this.currentPageSize  = params['size']      ? +params['size']     : DEFAULT_PAGE_SIZE;
      this.sortActive    = params['sortField'] ?? DEFAULT_SORT_FIELD;
      this.sortDirection = (params['sortDir']  ?? DEFAULT_SORT_DIR) as 'asc' | 'desc';
      this.loadPage();
    });

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

  protected onSortChange(sort: Sort): void {
    this.sortActive    = sort.active;
    this.sortDirection = (sort.direction || DEFAULT_SORT_DIR) as 'asc' | 'desc';
    this.navigate({ sortField: this.sortActive, sortDir: this.sortDirection, page: 0 });
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

  protected onProgramChange(value: InfiniteSelectValue | null): void {
    const val = value != null ? String(value) : 'ALL';
    this.filterProgram.set(val);
    this.navigate({ program: val === 'ALL' ? null : val, page: 0 });
  }

  protected onAcademicYearChange(value: InfiniteSelectValue | null): void {
    const val = value != null ? String(value) : 'ALL';
    this.filterAcademicYear.set(val);
    this.navigate({ academicYear: val === 'ALL' ? null : val, page: 0 });
  }

  protected onYearOfStudyChange(value: InfiniteSelectValue | null): void {
    const val = value != null ? String(value) : 'ALL';
    this.filterYearOfStudy.set(val);
    this.navigate({ yearOfStudy: val === 'ALL' ? null : val, page: 0 });
  }

  protected onAllocStatusChange(value: InfiniteSelectValue | null): void {
    const val = value != null ? String(value) : 'ALL';
    this.filterAllocStatus.set(val);
    this.navigate({ allocationStatus: val === 'ALL' ? null : val, page: 0 });
  }

  protected clearAllFilters(): void {
    this.searchValue.set('');
    this.navigate({
      search: null, program: null, academicYear: null, yearOfStudy: null, allocationStatus: null, page: 0,
    });
  }

  protected viewDetails(student: StudentFeeSummary): void {
    void this.router.navigate(['/student-fees', student.studentId], {
      queryParams: { returnTo: 'fee-explorer' },
    });
  }

  private navigate(patch: Partial<{
    search: string | null; program: string | null; academicYear: string | null;
    yearOfStudy: string | null; allocationStatus: string | null;
    page: number; size: number; sortField: string; sortDir: string;
  }>): void {
    const cur = this.route.snapshot.queryParams;
    const p: Record<string, string | number | null> = {
      search:           'search'           in patch ? patch.search ?? null           : this.searchValue() || null,
      program:          'program'          in patch ? patch.program ?? null          : (cur['program'] ?? null),
      academicYear:     'academicYear'     in patch ? patch.academicYear ?? null     : (cur['academicYear'] ?? null),
      yearOfStudy:      'yearOfStudy'      in patch ? patch.yearOfStudy ?? null      : (cur['yearOfStudy'] ?? null),
      allocationStatus: 'allocationStatus' in patch ? patch.allocationStatus ?? null : (cur['allocationStatus'] ?? null),
      page:      'page'      in patch ? patch.page!          : this.currentPage,
      size:      'size'      in patch ? patch.size!          : this.currentPageSize,
      sortField: 'sortField' in patch ? (patch.sortField ?? null) : (cur['sortField'] ?? null),
      sortDir:   'sortDir'   in patch ? (patch.sortDir ?? null)   : (cur['sortDir'] ?? null),
    };
    const qp: Record<string, string | number> = {};
    if (p['search'])           qp['search']           = p['search'] as string;
    if (p['program'])          qp['program']          = p['program'] as string;
    if (p['academicYear'])     qp['academicYear']     = p['academicYear'] as string;
    if (p['yearOfStudy'])      qp['yearOfStudy']      = p['yearOfStudy'] as string;
    if (p['allocationStatus']) qp['allocationStatus'] = p['allocationStatus'] as string;
    if ((p['page'] as number) > 0)    qp['page']   = p['page'] as number;
    if ((p['size'] as number) !== DEFAULT_PAGE_SIZE) qp['size'] = p['size'] as number;
    if (p['sortField']) qp['sortField'] = p['sortField'] as string;
    if (p['sortDir'])   qp['sortDir']   = p['sortDir'] as string;
    void this.router.navigate([], { relativeTo: this.route, queryParams: qp });
  }

  private loadFilterOptions(): void {
    this.financeService.getFeeExplorerFilterOptions().subscribe(opts => {
      this.programs.set(opts.programs);
      this.academicYears.set(opts.academicYears);
      this.yearsOfStudy.set(opts.yearsOfStudy);
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    const program      = this.filterProgram();
    const academicYear = this.filterAcademicYear();
    const yearOfStudy  = this.filterYearOfStudy();
    const allocStatus  = this.filterAllocStatus();
    this.financeService.searchStudentFeesPage({
      search:           this.searchValue() || undefined,
      page:             this.currentPage,
      size:             this.currentPageSize,
      sort:             `${SORT_FIELD_MAP[this.sortActive] ?? this.sortActive},${this.sortDirection}`,
      program:          program      !== 'ALL' ? program : null,
      academicYear:     academicYear !== 'ALL' ? academicYear : null,
      yearOfStudy:      yearOfStudy  !== 'ALL' ? Number(yearOfStudy) : null,
      allocationStatus: allocStatus  !== 'ALL' ? allocStatus : null,
    }).subscribe({
      next: (page) => {
        this.dataSource.data  = page.content;
        this.totalElements    = page.totalElements;
        this.currentPage      = page.number;
        this.currentPageSize  = page.size;
        this.syncPaginatorState();
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load student fees');
        this.loading.set(false);
      },
    });
  }

  private syncPaginatorState(): void {
    if (!this._paginator) return;
    this._paginator.length    = this.totalElements;
    this._paginator.pageIndex = this.currentPage;
    this._paginator.pageSize  = this.currentPageSize;
  }
}
