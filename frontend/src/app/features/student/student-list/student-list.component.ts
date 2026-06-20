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
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TitleCasePipe } from '@angular/common';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, takeUntil } from 'rxjs/operators';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, MatSort, SortDirection } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { StudentService } from '../student.service';
import { Student, StudentExplorerParams } from '../student.model';
import { ProgramService } from '../../program/program.service';
import { CourseService } from '../../course/course.service';
import { AcademicYearService } from '../../academic-year/academic-year.service';
import { Program } from '../../program/program.model';
import { Course } from '../../course/course.model';
import { AcademicYear } from '../../academic-year/academic-year.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { STUDENT_LIST_TOUR } from '../../../shared/tour/tours/student.tours';
import { ToastService } from '../../../core/toast/toast.service';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { computeInitials } from '../../../shared/utils/initials';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';

const DEFAULT_PAGE_SIZE = 25;
const SEARCH_MIN_LENGTH = 3;
const SORT_FIELD_MAP: Record<string, string> = {
  fullName:        'firstName',
  admissionNumber: 'admissionNumber',
  rollNumber:      'rollNumber',
  programName:     'program.name',
  yearOfStudy:     'semester',
  admissionDate:   'admissionDate',
  status:          'status',
};

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [
    FormsModule,
    TitleCasePipe,
    AppDatePipe,
    RouterLink,
    CmsEmptyStateComponent,
    CmsTourButtonComponent,
    CmsRowActionButtonComponent,
    CmsStatusBadgeComponent,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
  ],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.scss',
})
export class StudentListComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly studentService      = inject(StudentService);
  private readonly programService      = inject(ProgramService);
  private readonly courseService       = inject(CourseService);
  private readonly academicYearService = inject(AcademicYearService);
  private readonly router              = inject(Router);
  private readonly route               = inject(ActivatedRoute);
  private readonly toast               = inject(ToastService);
  private readonly dialog              = inject(MatDialog);
  private readonly tourService         = inject(TourService);
  private readonly _destroyRef         = inject(DestroyRef);

  @ViewChild(MatPaginator) paginator?: MatPaginator;
  @ViewChild(MatSort) matSort?: MatSort;

  protected readonly computeInitials = computeInitials;

  // ── Column visibility ────────────────────────────────────────
  protected readonly ALL_COLS = [
    'admissionNumber', 'rollNumber', 'fullName', 'programName', 'yearOfStudy',
    'admissionDate', 'phone', 'email', 'universityRegistrationNumber', 'labBatch',
    'status', 'actions',
  ];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    admissionNumber:              'Admission No.',
    rollNumber:                   'Roll No.',
    fullName:                     'Name',
    programName:                  'Program',
    yearOfStudy:                  'Sem',
    admissionDate:                'Admission Date',
    phone:                        'Phone',
    email:                        'Email',
    universityRegistrationNumber: 'Univ. Reg. No.',
    labBatch:                     'Lab Batch',
    status:                       'Status',
    actions:                      'Actions',
  };
  private readonly DEFAULT_COLS = new Set([
    'admissionNumber', 'fullName', 'programName', 'yearOfStudy', 'phone', 'status', 'actions',
  ]);
  private readonly COLS_KEY = 'student-list-cols-v2';
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() =>
    this.ALL_COLS.filter(c => this._visibleCols().has(c)),
  );

  // ── Table (server-side — paginator/sort NOT wired to dataSource) ──
  protected readonly dataSource = new MatTableDataSource<Student>([]);
  protected readonly loading    = signal(false);
  protected totalElements       = 0;

  // ── Master data for filter dropdowns ────────────────────────
  protected programs: Program[]           = [];
  protected allCourses: Course[]          = [];
  protected academicYears: AcademicYear[] = [];
  protected readonly filteredCourses = computed(() => {
    const pid = this.filterProgramId();
    return pid ? this.allCourses.filter(c => c.program?.id === pid) : this.allCourses;
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
  private currentSortField = 'admissionNumber';
  private currentSortDir: SortDirection = 'asc';

  private readonly destroy$     = new Subject<void>();
  private readonly searchSubject = new Subject<string>();

  protected colMenuOpen = false;

  ngOnInit(): void {
    this.tourService.register('student-list', STUDENT_LIST_TOUR);
    this.loadMasterData();

    // Single subscriber for URL params — handles initial load + back-navigation
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.filterProgramId.set(params['programId']      ? +params['programId']      : null);
      this.filterCourseId.set(params['courseId']        ? +params['courseId']        : null);
      this.filterAcademicYearId.set(params['academicYearId'] ? +params['academicYearId'] : null);
      this.filterStatus.set(params['status']            ?? '');
      this.filterStudentType.set(params['studentType']  ?? '');
      this.searchTerm.set(params['search']              ?? '');
      this.currentPage      = params['page']      ? +params['page']      : 0;
      this.currentPageSize  = params['size']      ? +params['size']      : DEFAULT_PAGE_SIZE;
      this.currentSortField = params['sortField'] ?? 'admissionNumber';
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
    this.paginator?.page.pipe(takeUntil(this.destroy$)).subscribe((ev: PageEvent) => {
      this.navigate({ page: ev.pageIndex, size: ev.pageSize });
    });
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
    const params: StudentExplorerParams = {
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

    this.studentService.getExplorer(params).subscribe({
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
        this.toast.error('Failed to load students');
        this.loading.set(false);
      },
    });
  }

  // ── Row actions ───────────────────────────────────────────────
  protected viewStudent(student: Student): void {
    void this.router.navigate(['/students', student.id]);
  }

  protected editStudent(student: Student): void {
    void this.router.navigate(['/students', student.id, 'edit']);
  }

  protected deleteStudent(student: Student): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title:       'Delete Student',
        message:     `Are you sure you want to delete "${student.fullName}"?`,
        confirmText: 'Delete',
        cancelText:  'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.studentService.delete(student.id).subscribe({
          next:  () => { this.toast.success('Student deleted successfully'); this.loadPage(); },
          error: (err) => this.toast.error(err?.error?.message ?? 'Failed to delete student'),
        });
      }
    });
  }
}
