import {
  Component, computed, inject, OnDestroy, OnInit, signal, ViewChild,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, MatSort, SortDirection } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';

import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { TourService } from '../../../shared/tour/tour.service';
import { DOCUMENT_VERIFICATION_LIST_TOUR } from '../../../shared/tour/tours/enquiry.tours';
import { EnquiryService } from '../enquiry.service';
import { Enquiry } from '../enquiry.model';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ToastService } from '../../../core/toast/toast.service';
import { computeInitials } from '../../../shared/utils/initials';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { CmsIconViewComponent } from '../../../shared/icons';
import { ProgramService } from '../../program/program.service';
import { CourseService } from '../../course/course.service';
import { Program } from '../../program/program.model';
import { Course } from '../../course/course.model';
import { ColumnPickerState, CmsColumnPickerComponent } from '../../../shared/column-picker';

const DEFAULT_PAGE_SIZE = 25;
const DEFAULT_SORT_FIELD = 'enquiryDate';
const DEFAULT_SORT_DIR: SortDirection = 'desc';
const SORT_FIELD_MAP: Record<string, string> = {
  name:        'name',
  enquiryDate: 'enquiryDate',
  studentType: 'studentType',
  programName: 'program.name',
  courseName:  'course.name',
};

@Component({
  selector: 'app-document-verification-list',
  standalone: true,
  imports: [
    FormsModule, AppDatePipe,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
    CmsRowActionButtonComponent,
    CmsTypeBadgeComponent,
    CmsIconViewComponent,
  ],
  templateUrl: './document-verification-list.component.html',
  styleUrl: './document-verification-list.component.scss',
})
export class DocumentVerificationListComponent implements OnInit, OnDestroy {
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

  @ViewChild(MatSort)
  set matSort(value: MatSort | undefined) {
    if (this._matSort === value) return;
    this._matSortSub?.unsubscribe();
    this._matSort = value;
    if (!value) return;
    this._matSortSub = value.sortChange.pipe(takeUntil(this.destroy$)).subscribe(ev => {
      const backendField = SORT_FIELD_MAP[ev.active];
      if (!backendField) return;
      const dir = (ev.direction || DEFAULT_SORT_DIR) as SortDirection;
      this.navigate({ sortField: backendField, sortDir: dir, page: 0 });
    });
    value.active    = this.currentSortField;
    value.direction = this.currentSortDir;
  }
  get matSort(): MatSort | undefined { return this._matSort; }
  private _matSort?: MatSort;
  private _matSortSub?: Subscription;

  protected readonly loading     = signal(false);
  protected readonly searchQuery = signal('');

  protected readonly dataSource = new MatTableDataSource<Enquiry>([]);

  // ── Server-side pagination / sort state ───────────────────────────────────
  protected totalElements  = 0;
  private currentPage      = 0;
  private currentPageSize  = DEFAULT_PAGE_SIZE;
  private currentSortField = DEFAULT_SORT_FIELD;
  private currentSortDir: SortDirection = DEFAULT_SORT_DIR;

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
    storageKey: 'doc-verification-list-cols-v1',
    columns: [
      { key: 'name', label: 'Student', mandatory: true },
      { key: 'programName', label: 'Program' },
      { key: 'courseName', label: 'Course' },
      { key: 'studentType', label: 'Type' },
      { key: 'enquiryDate', label: 'Date' },
      { key: 'actions', label: 'Actions', mandatory: true, pinnable: false },
    ],
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());

  protected readonly computeInitials = computeInitials;

  private readonly destroy$      = new Subject<void>();
  private readonly searchSubject = new Subject<string>();

  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }

  ngOnInit(): void {
    this.tourService.register('document-verification-list', DOCUMENT_VERIFICATION_LIST_TOUR);
    this.loadMasterData();

    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.searchQuery.set(params['search'] ?? '');
      this.filterProgramId.set(params['programId'] ? +params['programId'] : null);
      this.filterCourseId.set(params['courseId'] ? +params['courseId'] : null);
      this.filterStudentType.set(params['studentType'] ?? '');
      this.currentPage      = params['page']      ? +params['page']     : 0;
      this.currentPageSize  = params['size']      ? +params['size']     : DEFAULT_PAGE_SIZE;
      this.currentSortField = params['sortField'] ?? DEFAULT_SORT_FIELD;
      this.currentSortDir   = (params['sortDir']  ?? DEFAULT_SORT_DIR) as SortDirection;
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
    this.enquiryService.getDocumentVerificationPendingPage({
      search:      this.searchQuery().length >= 2 ? this.searchQuery() : undefined,
      programId:   this.filterProgramId(),
      courseId:    this.filterCourseId(),
      studentType: this.filterStudentType() || undefined,
      page:        this.currentPage,
      size:        this.currentPageSize,
      sort:        `${this.currentSortField},${this.currentSortDir}`,
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

  // ── Column prefs ──────────────────────────────────────────────────────────



  protected canVerifyDocuments(): boolean {
    return this.permissionService.has('DOCUMENT_VERIFICATION_MANAGE');
  }

  protected viewEnquiry(item: Enquiry): void    { void this.router.navigate(['/enquiries', item.id]); }
  protected verifyDocuments(item: Enquiry): void { void this.router.navigate(['/enquiries/document-verification', item.id]); }

  protected startTour(): void {
    this.tourService.start('document-verification-list');
  }
}
