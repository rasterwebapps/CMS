import {
  Component, computed, inject, OnDestroy, OnInit, signal, ViewChild,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
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

const DEFAULT_PAGE_SIZE = 25;

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

  // ── Column visibility ─────────────────────────────────────────────────────
  protected readonly ALL_COLS = ['name', 'programName', 'courseName', 'studentType', 'enquiryDate', 'actions'];
  protected readonly COLUMN_LABELS: Record<string, string> = {
    name: 'Student', programName: 'Program', courseName: 'Course',
    studentType: 'Type', enquiryDate: 'Submitted', actions: 'Actions',
  };
  private readonly COLS_KEY     = 'doc-verification-list-cols-v1';
  private readonly DEFAULT_COLS = new Set(['name', 'programName', 'courseName', 'studentType', 'enquiryDate', 'actions']);
  private readonly _visibleCols = signal<Set<string>>(this._loadColPrefs());
  protected readonly displayedColumns = computed(() => this.ALL_COLS.filter(c => this._visibleCols().has(c)));

  protected readonly computeInitials = computeInitials;

  private readonly destroy$      = new Subject<void>();
  private readonly searchSubject = new Subject<string>();

  ngOnInit(): void {
    this.tourService.register('document-verification-list', DOCUMENT_VERIFICATION_LIST_TOUR);
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

  // ── Column prefs ──────────────────────────────────────────────────────────
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

  protected canVerifyDocuments(): boolean {
    return this.permissionService.has('DOCUMENT_VERIFICATION_MANAGE');
  }

  protected viewEnquiry(item: Enquiry): void    { void this.router.navigate(['/enquiries', item.id]); }
  protected verifyDocuments(item: Enquiry): void { void this.router.navigate(['/enquiries/document-verification', item.id]); }

  protected startTour(): void {
    this.tourService.start('document-verification-list');
  }
}
