import {
  AfterViewInit, Component, computed, inject, OnDestroy, OnInit, signal, ViewChild,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
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

const DEFAULT_PAGE_SIZE = 25;

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
  ],
  templateUrl: './admission-completion-list.component.html',
  styleUrl: './admission-completion-list.component.scss',
})
export class AdmissionCompletionListComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly enquiryService    = inject(EnquiryService);
  private readonly permissionService = inject(PermissionService);
  private readonly programService    = inject(ProgramService);
  private readonly courseService     = inject(CourseService);
  private readonly router            = inject(Router);
  private readonly route             = inject(ActivatedRoute);
  private readonly toast             = inject(ToastService);
  private readonly tourService       = inject(TourService);

  @ViewChild(MatPaginator) paginator?: MatPaginator;

  protected readonly loading     = signal(false);
  protected readonly searchQuery = signal('');
  protected colMenuOpen          = false;

  protected readonly dataSource = new MatTableDataSource<Enquiry>([]);

  // ── Server-side pagination state ──────────────────────────────────────────
  protected totalElements  = 0;
  private currentPage      = 0;
  private currentPageSize  = DEFAULT_PAGE_SIZE;

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

  // ── Column visibility ──────────────────────────────────────────────────────
  protected readonly ALL_COLS = ['name', 'programName', 'courseName', 'status', 'totalPaidAmount', 'finalizedNetFee', 'finalizedAt', 'enquiryDate', 'actions'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    name: 'Student', programName: 'Program', courseName: 'Course',
    status: 'Status', totalPaidAmount: 'Paid (₹)', finalizedNetFee: 'Net Fee (₹)',
    finalizedAt: 'Fees Finalized', enquiryDate: 'Enquiry Date', actions: 'Actions',
  };
  private readonly COLS_KEY     = 'admission-completion-list-cols-v2';
  private readonly DEFAULT_COLS = new Set(['name', 'programName', 'courseName', 'status', 'totalPaidAmount', 'finalizedNetFee', 'finalizedAt', 'actions']);
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));

  protected readonly computeInitials = computeInitials;

  private readonly destroy$      = new Subject<void>();
  private readonly searchSubject = new Subject<string>();

  ngOnInit(): void {
    this.tourService.register('admission-completion-list', ADMISSION_COMPLETION_LIST_TOUR);
    this.loadMasterData();

    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.searchQuery.set(params['search'] ?? '');
      this.filterProgramId.set(params['programId'] ? +params['programId'] : null);
      this.filterCourseId.set(params['courseId'] ? +params['courseId'] : null);
      this.filterStudentType.set(params['studentType'] ?? '');
      this.currentPage     = params['page'] ? +params['page'] : 0;
      this.currentPageSize = params['size'] ? +params['size'] : DEFAULT_PAGE_SIZE;
      this.loadPage();
    });

    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(val => this.navigate({ search: val || null, page: 0 }));
  }

  ngAfterViewInit(): void {
    this.paginator?.page.pipe(takeUntil(this.destroy$)).subscribe((ev: PageEvent) => {
      this.navigate({ page: ev.pageIndex, size: ev.pageSize });
    });
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
    }).subscribe({
      next: page => {
        this.dataSource.data = page.content;
        this.totalElements   = page.totalElements;
        if (this.paginator) {
          this.paginator.length    = page.totalElements;
          this.paginator.pageIndex = page.number;
          this.paginator.pageSize  = page.size;
        }
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
    studentType: string | null; page: number; size: number;
  }>): void {
    const cur = this.route.snapshot.queryParams;
    const merged = {
      search:      'search'      in patch ? patch.search      : (cur['search'] ?? null),
      programId:   'programId'   in patch ? patch.programId   : (cur['programId'] ?? null),
      courseId:    'courseId'    in patch ? patch.courseId    : (cur['courseId'] ?? null),
      studentType: 'studentType' in patch ? patch.studentType : (cur['studentType'] ?? null),
      page:        'page'        in patch ? patch.page        : this.currentPage,
      size:        'size'        in patch ? patch.size        : this.currentPageSize,
    };
    const queryParams = Object.fromEntries(
      Object.entries(merged).filter(([, v]) => v !== null && v !== undefined && v !== ''),
    );
    void this.router.navigate([], { relativeTo: this.route, queryParams });
  }

  // ── Column prefs ───────────────────────────────────────────────────────────
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
      if (next.size > 1 && next.has(col)) { next.delete(col); } else { next.add(col); }
      localStorage.setItem(this.COLS_KEY, JSON.stringify([...next]));
      return next;
    });
  }

  protected isColumnVisible(col: string): boolean { return this._visibleCols().has(col); }

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
