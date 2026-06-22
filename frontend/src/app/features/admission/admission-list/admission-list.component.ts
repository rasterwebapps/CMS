import {
  AfterViewInit,
  Component,
  computed,
  DestroyRef,
  inject,
  OnDestroy,
  OnInit,
  signal,
  ViewChild,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TitleCasePipe } from '@angular/common';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, takeUntil } from 'rxjs/operators';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, MatSort, SortDirection } from '@angular/material/sort';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { AdmissionService } from '../admission.service';
import { AdmissionExplorerParams, AdmissionResponse } from '../admission.model';
import { ProgramService } from '../../program/program.service';
import { CourseService } from '../../course/course.service';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { Program } from '../../program/program.model';
import { Course } from '../../course/course.model';
import { AcademicYear } from '../../academic-year/academic-year.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ADMISSION_LIST_TOUR } from '../../../shared/tour/tours/admission.tours';
import { computeInitials } from '../../../shared/utils/initials';
import { CmsIconDeleteComponent, CmsIconEditComponent, CmsIconViewComponent } from '../../../shared/icons';

const DEFAULT_PAGE_SIZE = 25;
const SEARCH_MIN_LENGTH = 3;
const SORT_FIELD_MAP: Record<string, string> = {
  studentName:     'student.firstName',
  admissionNumber: 'student.admissionNumber',
  rollNumber:      'student.rollNumber',
  program:         'student.program.name',
  course:          'student.course.name',
  yearOfStudy:     'student.semester',
  applicationDate: 'applicationDate',
  academicYear:    'joiningAcademicYear.name',
  studentStatus:   'student.status',
};

@Component({
  selector: 'app-admission-list',
  standalone: true,
  imports: [
    FormsModule,
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
    CmsRowActionButtonComponent,
    CmsStatusBadgeComponent,
    CmsIconDeleteComponent,
  CmsIconEditComponent,
  CmsIconViewComponent,
],
  templateUrl: './admission-list.component.html',
  styleUrl: './admission-list.component.scss',
})
export class AdmissionListComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly admissionService    = inject(AdmissionService);
  private readonly programService      = inject(ProgramService);
  private readonly courseService       = inject(CourseService);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly router              = inject(Router);
  private readonly route               = inject(ActivatedRoute);
  private readonly toast               = inject(ToastService);
  private readonly dialog              = inject(MatDialog);
  private readonly tourService         = inject(TourService);
  // DestroyRef kept for future signal-based teardown
  private readonly _destroyRef         = inject(DestroyRef);

  @ViewChild(MatPaginator) paginator?: MatPaginator;
  @ViewChild(MatSort) matSort?: MatSort;

  protected readonly computeInitials = computeInitials;

  // ── Column visibility ────────────────────────────────────────
  protected readonly ALL_COLS = [
    'studentName', 'admissionNumber', 'rollNumber', 'program', 'course',
    'yearOfStudy', 'applicationDate', 'academicYear', 'consent', 'declarationDate', 'studentStatus', 'actions',
  ];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    studentName:     'Student',
    admissionNumber: 'Admission No.',
    rollNumber:      'Roll No.',
    program:         'Program',
    course:          'Course',
    yearOfStudy:     'Year',
    applicationDate: 'Application Date',
    academicYear:    'Joining Year',
    consent:         'Consent',
    declarationDate: 'Declaration Date',
    studentStatus:   'Status',
    actions:         'Actions',
  };
  private readonly DEFAULT_COLS = new Set([
    'studentName', 'admissionNumber', 'program', 'course', 'yearOfStudy',
    'applicationDate', 'academicYear', 'consent', 'studentStatus', 'actions',
  ]);
  private readonly COLS_KEY = 'admission-list-cols-v3';
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() =>
    this.ALL_COLS.filter(c => this._visibleCols().has(c)),
  );

  // ── Table (server-side — paginator/sort NOT wired to dataSource) ──
  protected readonly dataSource = new MatTableDataSource<AdmissionResponse>([]);
  protected readonly loading    = signal(false);
  protected totalElements       = 0;

  // ── Master data for filter dropdowns ────────────────────────
  protected programs: Program[]           = [];
  protected allCourses: Course[]          = [];
  protected academicYears: AcademicYear[] = [];
  protected readonly filteredCourses = computed(() => {
    const pid = this.filterProgramId();
    return pid ? this.allCourses.filter(c => c.program.id === pid) : this.allCourses;
  });

  // ── Filter state ─────────────────────────────────────────────
  protected readonly filterProgramId      = signal<number | null>(null);
  protected readonly filterCourseId       = signal<number | null>(null);
  protected readonly filterAcademicYearId = signal<number | null>(null);
  protected readonly filterStatus         = signal<string>('');
  protected readonly filterStudentType    = signal<string>('');
  protected readonly searchTerm           = signal<string>('');

  protected readonly STUDENT_STATUSES = [
    'ACTIVE', 'INACTIVE', 'GRADUATED', 'ON_LEAVE', 'SUSPENDED', 'WITHDRAWN', 'EXPELLED',
  ];
  protected readonly STUDENT_TYPES = [
    { value: 'DAY_SCHOLAR', label: 'Day Scholar' },
    { value: 'HOSTELER',    label: 'Hosteler' },
  ];

  protected readonly hasActiveFilters = computed(() =>
    !!this.filterProgramId() ||
    !!this.filterCourseId() ||
    !!this.filterAcademicYearId() ||
    !!this.filterStatus() ||
    !!this.filterStudentType() ||
    this.searchTerm().length >= SEARCH_MIN_LENGTH,
  );

  // ── Pagination / sort state ──────────────────────────────────
  private currentPage      = 0;
  private currentPageSize  = DEFAULT_PAGE_SIZE;
  private currentSortField = 'student.admissionNumber';
  private currentSortDir: SortDirection = 'asc';

  private readonly destroy$ = new Subject<void>();
  private readonly searchSubject = new Subject<string>();

  protected colMenuOpen = false;

  ngOnInit(): void {
    this.tourService.register('admission-list', ADMISSION_LIST_TOUR);
    this.loadMasterData();

    // Single subscriber for URL params — handles initial load + back-navigation
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.filterProgramId.set(params['programId'] ? +params['programId'] : null);
      this.filterCourseId.set(params['courseId'] ? +params['courseId'] : null);
      this.filterAcademicYearId.set(params['academicYearId'] ? +params['academicYearId'] : null);
      this.filterStatus.set(params['status'] ?? '');
      this.filterStudentType.set(params['studentType'] ?? '');
      this.searchTerm.set(params['search'] ?? '');
      this.currentPage      = params['page']      ? +params['page']      : 0;
      this.currentPageSize  = params['size']      ? +params['size']      : DEFAULT_PAGE_SIZE;
      this.currentSortField = params['sortField'] ?? 'student.admissionNumber';
      this.currentSortDir   = (params['sortDir']  ?? 'asc') as SortDirection;
      this.loadPage();
    });

    // Search: debounce 400 ms, require ≥ 3 chars or empty (clear)
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      filter(v => v.length === 0 || v.length >= SEARCH_MIN_LENGTH),
      takeUntil(this.destroy$),
    ).subscribe(val => {
      this.navigate({ search: val || null, page: 0 });
    });
  }

  ngAfterViewInit(): void {
    // Paginator page events → URL update
    this.paginator?.page.pipe(takeUntil(this.destroy$)).subscribe((ev: PageEvent) => {
      this.navigate({ page: ev.pageIndex, size: ev.pageSize });
    });

    // Sort column/direction events → URL update, reset to page 0
    this.matSort?.sortChange.pipe(takeUntil(this.destroy$)).subscribe(ev => {
      const field = SORT_FIELD_MAP[ev.active] ?? ev.active;
      const dir   = (ev.direction || 'asc') as SortDirection;
      this.navigate({ sortField: field, sortDir: dir, page: 0 });
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Filter change handlers ────────────────────────────────────
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

  protected onAcademicYearChange(val: string): void {
    const ayId = val ? +val : null;
    this.filterAcademicYearId.set(ayId);
    this.navigate({ academicYearId: ayId, page: 0 });
  }

  protected onStatusChange(val: string): void {
    this.filterStatus.set(val);
    this.navigate({ status: val || null, page: 0 });
  }

  protected onStudentTypeChange(val: string): void {
    this.filterStudentType.set(val);
    this.navigate({ studentType: val || null, page: 0 });
  }

  protected onSearch(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.searchTerm.set(val);
    this.searchSubject.next(val);
  }

  protected clearSearch(): void {
    this.searchTerm.set('');
    this.searchSubject.next('');
  }

  protected clearAllFilters(): void {
    this.navigate({
      programId: null, courseId: null, academicYearId: null,
      status: null, studentType: null, search: null, page: 0,
    });
  }

  // ── Column visibility ─────────────────────────────────────────
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

  // ── URL state management (single source of truth) ─────────────
  private navigate(patch: Partial<{
    programId: number | null; courseId: number | null; academicYearId: number | null;
    status: string | null; studentType: string | null; search: string | null;
    page: number; size: number; sortField: string; sortDir: string;
  }>): void {
    const cur = this.route.snapshot.queryParams;
    const merged: Record<string, unknown> = {
      programId:      'programId'      in patch ? patch.programId      : (cur['programId'] ?? null),
      courseId:       'courseId'       in patch ? patch.courseId       : (cur['courseId'] ?? null),
      academicYearId: 'academicYearId' in patch ? patch.academicYearId : (cur['academicYearId'] ?? null),
      status:         'status'         in patch ? patch.status         : (cur['status'] ?? null),
      studentType:    'studentType'    in patch ? patch.studentType    : (cur['studentType'] ?? null),
      search:         'search'         in patch ? patch.search         : (cur['search'] ?? null),
      page:           'page'           in patch ? patch.page           : this.currentPage,
      size:           'size'           in patch ? patch.size           : this.currentPageSize,
      sortField:      'sortField'      in patch ? patch.sortField      : this.currentSortField,
      sortDir:        'sortDir'        in patch ? patch.sortDir        : this.currentSortDir,
    };
    // Strip nulls/empty so URL stays clean
    const queryParams = Object.fromEntries(
      Object.entries(merged).filter(([, v]) => v !== null && v !== undefined && v !== ''),
    );
    void this.router.navigate([], { relativeTo: this.route, queryParams });
  }

  // ── Data loading ──────────────────────────────────────────────
  private loadMasterData(): void {
    this.programService.getAll().subscribe(list => {
      this.programs = list.filter(p => (p.status as string) === 'ACTIVE');
    });
    this.courseService.getAll().subscribe(list => { this.allCourses = list; });
    this.academicYearService.getAllAcademicYears().subscribe(list => { this.academicYears = list; });
  }

  private loadPage(): void {
    this.loading.set(true);
    const params: AdmissionExplorerParams = {
      programId:      this.filterProgramId()      ?? undefined,
      courseId:       this.filterCourseId()       ?? undefined,
      academicYearId: this.filterAcademicYearId() ?? undefined,
      status:         this.filterStatus()  || undefined,
      studentType:    this.filterStudentType() || undefined,
      search:         this.searchTerm().length >= SEARCH_MIN_LENGTH ? this.searchTerm() : undefined,
      page:           this.currentPage,
      size:           this.currentPageSize,
      sort:           `${this.currentSortField},${this.currentSortDir || 'asc'}`,
    };

    this.admissionService.getExplorer(params).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements   = page.totalElements;
        if (this.paginator) {
          this.paginator.length    = page.totalElements;
          this.paginator.pageIndex = page.number;
          this.paginator.pageSize  = page.size;
        }
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load admissions');
        this.loading.set(false);
      },
    });
  }

  // ── Row actions ───────────────────────────────────────────────
  protected view(item: AdmissionResponse): void {
    void this.router.navigate(['/admissions', item.id]);
  }

  protected canEdit(item: AdmissionResponse): boolean {
    return !['GRADUATED', 'WITHDRAWN', 'EXPELLED'].includes(item.studentStatus ?? '');
  }

  protected edit(item: AdmissionResponse): void {
    void this.router.navigate(['/admissions', item.id, 'edit']);
  }

  protected delete(item: AdmissionResponse): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title:       'Delete Admission',
        message:     `Delete admission for "${item.studentName}"?`,
        confirmText: 'Delete',
        cancelText:  'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.admissionService.delete(item.id).subscribe({
          next:  () => { this.toast.success('Deleted'); this.loadPage(); },
          error: (err) => this.toast.error(err?.error?.message ?? 'Failed to delete'),
        });
      }
    });
  }
}
