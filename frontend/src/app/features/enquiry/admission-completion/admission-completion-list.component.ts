import {
  Component, computed, inject, OnDestroy, OnInit, signal, ViewChild,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';

import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { EnquiryService } from '../enquiry.service';
import { Enquiry } from '../enquiry.model';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { computeInitials } from '../../../shared/utils/initials';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ADMISSION_COMPLETION_LIST_TOUR } from '../../../shared/tour/tours/enquiry.tours';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsIconViewComponent } from '../../../shared/icons';
import { ProgramService } from '../../program/program.service';
import { CourseService } from '../../course/course.service';
import { Program } from '../../program/program.model';
import { Course } from '../../course/course.model';
import { ColumnPickerState, CmsColumnPickerComponent } from '../../../shared/column-picker';

const DEFAULT_PAGE_SIZE = 25;
const DEFAULT_SORT_FIELD = 'enquiryDate';
const DEFAULT_SORT_DIR: 'asc' | 'desc' = 'desc';
const SORT_FIELD_MAP: Record<string, string> = {
  name:        'name',
  enquiryDate: 'enquiryDate',
  finalizedAt: 'finalizedAt',
  status:      'status',
  programName: 'program.name',
  courseName:  'course.name',
  // totalPaidAmount deliberately excluded: computed in application code from
  // EnquiryPayment records, not a real Enquiry column the sort can target.
  finalizedNetFee: 'finalizedNetFee',
};

@Component({
  selector: 'app-admission-completion-list',
  standalone: true,
  imports: [
    InrPipe, AppDatePipe, FormsModule,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
    CmsTourButtonComponent,
    CmsRowActionButtonComponent,
    CmsIconViewComponent,
    CmsColumnPickerComponent,
  ],
  templateUrl: './admission-completion-list.component.html',
  styleUrl: './admission-completion-list.component.scss',
})
export class AdmissionCompletionListComponent implements OnInit, OnDestroy {
  private readonly enquiryService    = inject(EnquiryService);
  private readonly permissionService = inject(PermissionService);
  private readonly programService    = inject(ProgramService);
  private readonly courseService     = inject(CourseService);
  private readonly router            = inject(Router);
  private readonly route             = inject(ActivatedRoute);
  private readonly toast             = inject(ToastService);
  private readonly tourService       = inject(TourService);

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

  protected readonly loading     = signal(false);
  protected readonly searchQuery = signal('');

  protected readonly dataSource = new MatTableDataSource<Enquiry>([]);

  // ── Server-side pagination / sort state ───────────────────────────────────
  protected totalElements  = 0;
  private currentPage      = 0;
  private currentPageSize  = DEFAULT_PAGE_SIZE;
  protected sortActive     = DEFAULT_SORT_FIELD;
  protected sortDirection: 'asc' | 'desc' = DEFAULT_SORT_DIR;

  // ── Filters ───────────────────────────────────────────────────────────────
  protected readonly filterProgramId   = signal<number | null>(null);
  protected readonly filterCourseId    = signal<number | null>(null);
  protected readonly filterStudentType = signal<string>('');

  // ── Master data ───────────────────────────────────────────────────────────
  protected programs:   Program[] = [];
  protected allCourses: Course[]  = [];

  protected readonly filteredCourses = computed(() => {
    const pid = this.filterProgramId();
    return pid ? this.allCourses.filter(c => c.program?.id === pid) : this.allCourses;
  });

  protected readonly colState = new ColumnPickerState({
    storageKey: 'admission-completion-list-cols-v2',
    columns: [
      { key: 'name', label: 'Student', mandatory: true },
      { key: 'programName', label: 'Program' },
      { key: 'courseName', label: 'Course' },
      { key: 'status', label: 'Status' },
      { key: 'totalPaidAmount', label: 'Paid' },
      { key: 'finalizedNetFee', label: 'Net Fee' },
      { key: 'finalizedAt', label: 'Fees Finalized' },
      { key: 'enquiryDate', label: 'Enquiry Date' },
      { key: 'actions', label: 'Actions', mandatory: true, pinnable: false },
    ],
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());

  protected readonly computeInitials = computeInitials;

  private readonly destroy$      = new Subject<void>();
  private readonly searchSubject = new Subject<string>();

  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }

  ngOnInit(): void {
    this.tourService.register('admission-completion-list', ADMISSION_COMPLETION_LIST_TOUR);
    this.loadMasterData();

    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.searchQuery.set(params['search'] ?? '');
      this.filterProgramId.set(params['programId'] ? +params['programId'] : null);
      this.filterCourseId.set(params['courseId'] ? +params['courseId'] : null);
      this.filterStudentType.set(params['studentType'] ?? '');
      this.currentPage      = params['page']      ? +params['page']     : 0;
      this.currentPageSize  = params['size']      ? +params['size']     : DEFAULT_PAGE_SIZE;
      this.sortActive    = params['sortField'] ?? DEFAULT_SORT_FIELD;
      this.sortDirection = (params['sortDir']  ?? DEFAULT_SORT_DIR) as 'asc' | 'desc';
      this.loadPage();
    });

    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(val => this.navigate({ search: val || null, page: 0 }));
  }

  private syncPaginatorState(): void {
    if (!this._paginator) return;
    this._paginator.length    = this.totalElements;
    this._paginator.pageIndex = this.currentPage;
    this._paginator.pageSize  = this.currentPageSize;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadMasterData(): void {
    this.programService.getAll().subscribe(list => {
      this.programs = list.filter(p => (p.status as string) === 'ACTIVE');
    });
    this.courseService.getAll().subscribe(list => { this.allCourses = list; });
  }

  private loadPage(): void {
    this.loading.set(true);
    this.enquiryService.getAdmissionPendingPage({
      search:      this.searchQuery().length >= 2 ? this.searchQuery() : undefined,
      programId:   this.filterProgramId(),
      courseId:    this.filterCourseId(),
      studentType: this.filterStudentType() || undefined,
      page:        this.currentPage,
      size:        this.currentPageSize,
      sort:        `${SORT_FIELD_MAP[this.sortActive] ?? this.sortActive},${this.sortDirection}`,
    }).subscribe({
      next: page => {
        this.dataSource.data = page.content;
        this.totalElements   = page.totalElements;
        this.currentPage     = page.number;
        this.currentPageSize = page.size;
        this.syncPaginatorState();
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load enquiries'); this.loading.set(false); },
    });
  }

  protected onSearch(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.searchQuery.set(val);
    this.searchSubject.next(val);
  }

  protected clearSearch(): void {
    this.searchQuery.set('');
    this.searchSubject.next('');
  }

  protected onProgramChange(val: string): void {
    const pid = val ? +val : null;
    this.filterProgramId.set(pid);
    this.filterCourseId.set(null);
    this.navigate({ programId: pid, courseId: null, page: 0 });
  }

  protected onCourseChange(val: string): void {
    const cid = val ? +val : null;
    this.filterCourseId.set(cid);
    this.navigate({ courseId: cid, page: 0 });
  }

  protected onStudentTypeChange(val: string): void {
    this.filterStudentType.set(val);
    this.navigate({ studentType: val || null, page: 0 });
  }

  protected clearFilters(): void {
    this.navigate({ search: null, programId: null, courseId: null, studentType: null, page: 0 });
  }

  protected hasActiveFilters(): boolean {
    return !!this.filterProgramId() || !!this.filterCourseId() ||
           !!this.filterStudentType() || this.searchQuery().length >= 2;
  }

  protected onSortChange(sort: Sort): void {
    this.sortActive    = sort.active;
    this.sortDirection = (sort.direction || DEFAULT_SORT_DIR) as 'asc' | 'desc';
    this.navigate({ sortField: this.sortActive, sortDir: this.sortDirection, page: 0 });
  }

  private navigate(patch: Partial<{
    search: string | null; programId: number | null; courseId: number | null;
    studentType: string | null; page: number; size: number; sortField: string | null; sortDir: string | null;
  }>): void {
    const cur = this.route.snapshot.queryParams;
    const merged = {
      search:      'search'      in patch ? patch.search      : (cur['search'] ?? null),
      programId:   'programId'   in patch ? patch.programId   : (cur['programId'] ?? null),
      courseId:    'courseId'    in patch ? patch.courseId    : (cur['courseId'] ?? null),
      studentType: 'studentType' in patch ? patch.studentType : (cur['studentType'] ?? null),
      page:        'page'        in patch ? patch.page        : this.currentPage,
      size:        'size'        in patch ? patch.size        : this.currentPageSize,
      sortField:   'sortField'   in patch ? patch.sortField   : (cur['sortField'] ?? null),
      sortDir:     'sortDir'     in patch ? patch.sortDir     : (cur['sortDir'] ?? null),
    };
    const queryParams = Object.fromEntries(
      Object.entries(merged).filter(([, v]) => v !== null && v !== undefined && v !== ''),
    );
    void this.router.navigate([], { relativeTo: this.route, queryParams });
  }

  // ── Column prefs ───────────────────────────────────────────────────────────



  protected canCompleteAdmission(): boolean {
    return this.permissionService.hasAny('ADMISSION_CREATE', 'ADMISSION_EDIT');
  }

  protected viewEnquiry(item: Enquiry): void { void this.router.navigate(['/enquiries', item.id]); }

  protected completeAdmission(item: Enquiry): void {
    if (item.status !== 'DOCUMENTS_VERIFIED') {
      this.toast.warning('Only enquiries with Documents Verified status can be completed.');
      return;
    }
    void this.router.navigate(['/enquiries', item.id, 'convert']);
  }
}
