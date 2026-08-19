import {
  Component, inject, OnDestroy, OnInit, signal, ViewChild, computed,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatIconModule } from '@angular/material/icon';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';

import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { SCHOLARSHIP_APPLICATIONS_TOUR, SCHOLARSHIP_APPLICATIONS_FLOW_MAP } from '../../../shared/tour/tours/student.tours';
import { ToastService } from '../../../core/toast/toast.service';
import { ScholarshipApplication } from '../scholarship.model';
import { ScholarshipService } from '../scholarship.service';
import { ScholarshipApproveDialogComponent } from '../approve-dialog/scholarship-approve-dialog.component';
import { ScholarshipRejectDialogComponent } from '../reject-dialog/scholarship-reject-dialog.component';
import { ColumnPickerState, CmsColumnPickerComponent } from '../../../shared/column-picker';

import { ColumnResizeDirective, CmsWrapTextToggleComponent } from '../../../shared/column-resize';
const DEFAULT_PAGE_SIZE = 25;
const SORT_FIELD_MAP: Record<string, string> = {
  studentName:      'student.firstName',
  scholarshipName:  'scholarshipType.name',
  academicYearName: 'academicYear.name',
  applicationDate:  'applicationDate',
  status:           'status',
  approvedAmount:   'approvedAmount',
};

@Component({
  selector: 'app-scholarship-applications-list',
  standalone: true,
  imports: [
    MatIconModule, MatDialogModule, MatTableModule, MatPaginatorModule, MatSortModule,
    AppDatePipe, InrPipe,
    CmsEmptyStateComponent, CmsStatusBadgeComponent, CmsRowActionButtonComponent,
    CmsTourButtonComponent,
    CmsColumnPickerComponent, ColumnResizeDirective, CmsWrapTextToggleComponent,
  ],
  templateUrl: './scholarship-applications-list.component.html',
  styleUrl: './scholarship-applications-list.component.scss',
})
export class ScholarshipApplicationsListComponent implements OnInit, OnDestroy {
  private readonly scholarshipService = inject(ScholarshipService);
  private readonly toast              = inject(ToastService);
  private readonly dialog             = inject(MatDialog);
  private readonly tourService        = inject(TourService);
  private readonly router             = inject(Router);
  private readonly route              = inject(ActivatedRoute);

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
  protected readonly dataSource  = new MatTableDataSource<ScholarshipApplication>([]);
  protected readonly colState = new ColumnPickerState({
    storageKey: 'scholarship-applications-columns',
    columns: [
      { key: 'studentName', label: 'Student', mandatory: true },
      { key: 'scholarshipName', label: 'Scholarship' },
      { key: 'academicYearName', label: 'Year' },
      { key: 'applicationDate', label: 'Date' },
      { key: 'status', label: 'Status' },
      { key: 'approvedAmount', label: 'Amount' },
      { key: 'actions', label: 'Actions', mandatory: true, pinnable: false },
    ],
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());

  protected totalElements  = 0;
  private currentPage      = 0;
  private currentPageSize  = DEFAULT_PAGE_SIZE;
  protected sortActive     = 'applicationDate';
  protected sortDirection: 'asc' | 'desc' = 'asc';

  protected readonly hasActiveFilters = computed(() => this.searchQuery().length >= 2);

  private readonly destroy$      = new Subject<void>();
  private readonly searchSubject = new Subject<string>();

  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }

  ngOnInit(): void {
    this.tourService.register('scholarship-applications', SCHOLARSHIP_APPLICATIONS_TOUR);
    this.tourService.registerFlowMap('scholarship-applications', SCHOLARSHIP_APPLICATIONS_FLOW_MAP);

    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.searchQuery.set(params['search'] ?? '');
      this.currentPage     = params['page'] ? +params['page'] : 0;
      this.currentPageSize = params['size'] ? +params['size'] : DEFAULT_PAGE_SIZE;
      this.sortActive      = params['sortField'] ?? 'applicationDate';
      this.sortDirection   = (params['sortDir']  ?? 'asc') as 'asc' | 'desc';
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

  private loadPage(): void {
    this.loading.set(true);
    this.scholarshipService.getPendingApplicationsPage({
      search: this.searchQuery().length >= 2 ? this.searchQuery() : undefined,
      page:   this.currentPage,
      size:   this.currentPageSize,
      sort:   `${SORT_FIELD_MAP[this.sortActive] ?? this.sortActive},${this.sortDirection}`,
    }).subscribe({
      next: page => {
        this.dataSource.data = page.content;
        this.totalElements   = page.totalElements;
        this.currentPage     = page.number;
        this.currentPageSize = page.size;
        this.syncPaginatorState();
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load applications'); this.loading.set(false); },
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

  protected onSortChange(sort: Sort): void {
    this.sortActive    = sort.active;
    this.sortDirection = (sort.direction || 'asc') as 'asc' | 'desc';
    this.navigate({ sortField: this.sortActive, sortDir: this.sortDirection, page: 0 });
  }

  protected approve(row: ScholarshipApplication): void {
    const ref = this.dialog.open(ScholarshipApproveDialogComponent, {
      width: '520px', maxWidth: '95vw', data: { application: row },
    });
    ref.afterClosed().subscribe((updated: ScholarshipApplication | undefined) => {
      if (updated) this.loadPage();
    });
  }

  protected reject(row: ScholarshipApplication): void {
    const ref = this.dialog.open(ScholarshipRejectDialogComponent, {
      width: '480px', maxWidth: '95vw', data: { application: row },
    });
    ref.afterClosed().subscribe((updated: ScholarshipApplication | undefined) => {
      if (updated) this.loadPage();
    });
  }

  private navigate(patch: Partial<{
    search: string | null; page: number; size: number; sortField: string | null; sortDir: string | null;
  }>): void {
    const cur = this.route.snapshot.queryParams;
    const merged = {
      search:    'search'    in patch ? patch.search    : (cur['search'] ?? null),
      page:      'page'      in patch ? patch.page      : this.currentPage,
      size:      'size'      in patch ? patch.size      : this.currentPageSize,
      sortField: 'sortField' in patch ? patch.sortField : (cur['sortField'] ?? null),
      sortDir:   'sortDir'   in patch ? patch.sortDir   : (cur['sortDir'] ?? null),
    };
    const queryParams = Object.fromEntries(
      Object.entries(merged).filter(([, v]) => v !== null && v !== undefined && v !== ''),
    );
    void this.router.navigate([], { relativeTo: this.route, queryParams });
  }
}
