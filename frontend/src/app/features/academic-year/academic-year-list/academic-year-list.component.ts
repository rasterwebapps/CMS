import { Component, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { AcademicYearService } from '../academic-year.service';
import { AcademicYear } from '../academic-year.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { ToastService } from '../../../core/toast/toast.service';
import { AppDatePipe } from '../../../shared/pipes/app-date.pipe';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { ACADEMIC_YEAR_LIST_TOUR, ACADEMIC_YEAR_LIST_FLOW_MAP } from '../../../shared/tour/tours/academic-year.tours';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsIconDeleteComponent, CmsIconEditComponent, CmsIconViewComponent } from '../../../shared/icons';

@Component({
  selector: 'app-academic-year-list',
  standalone: true,
  imports: [
    AppDatePipe,
    CmsEmptyStateComponent,
    CmsViewToggleComponent,
    CmsTourButtonComponent,
    CmsStatusBadgeComponent,
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
    CmsRowActionButtonComponent,
    CmsIconDeleteComponent,
    CmsIconEditComponent,
    CmsIconViewComponent,
  ],
  templateUrl: './academic-year-list.component.html',
  styleUrl: './academic-year-list.component.scss',
})
export class AcademicYearListComponent implements OnInit, OnDestroy {
  private readonly academicYearService = inject(AcademicYearService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  private readonly destroy$ = new Subject<void>();
  private readonly searchSubject = new Subject<string>();
  private _paginator?: MatPaginator;
  private _paginatorSub?: Subscription;

  @ViewChild(MatPaginator) set paginatorRef(p: MatPaginator | undefined) {
    if (!p || p === this._paginator) return;
    this._paginatorSub?.unsubscribe();
    this._paginator = p;
    p.pageIndex = this.currentPage;
    p.pageSize = this.currentPageSize;
    this._paginatorSub = p.page.pipe(takeUntil(this.destroy$)).subscribe((e: PageEvent) => {
      this.currentPage = e.pageIndex;
      this.currentPageSize = e.pageSize;
      this.loadPage();
    });
  }

  private readonly VIEW_MODE_KEY = 'academic-year-list-view-mode';

  protected readonly dataSource = new MatTableDataSource<AcademicYear>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());
  protected readonly isCurrentFilter = signal<boolean | null>(null);

  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;
  protected sortActive = 'name';
  protected sortDirection: 'asc' | 'desc' = 'asc';

  protected readonly displayedColumns: readonly string[] = ['name', 'startDate', 'endDate', 'isCurrent', 'actions'];

  private readonly sortMap: Record<string, string> = {
    name: 'name',
    startDate: 'startDate',
    endDate: 'endDate',
    isCurrent: 'isCurrent',
  };

  ngOnInit(): void {
    this.tourService.register('academic-year-list', ACADEMIC_YEAR_LIST_TOUR);
    this.tourService.registerFlowMap('academic-year-list', ACADEMIC_YEAR_LIST_FLOW_MAP);
    this.searchSubject.pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => { this.currentPage = 0; this.loadPage(); });
    this.loadPage();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this._paginatorSub?.unsubscribe();
  }

  protected setViewMode(mode: 'card' | 'table'): void {
    this.viewMode.set(mode);
    localStorage.setItem(this.VIEW_MODE_KEY, mode);
  }

  private loadViewMode(): 'card' | 'table' {
    const stored = localStorage.getItem(this.VIEW_MODE_KEY);
    return stored === 'table' ? 'table' : 'card';
  }

  protected navigateToNew(): void {
    void this.router.navigate(['/academic-years/new']);
  }

  protected applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.searchSubject.next(value);
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.searchSubject.next('');
  }

  protected onIsCurrentFilterChange(value: string): void {
    this.isCurrentFilter.set(value === '' ? null : value === 'true');
    this.currentPage = 0;
    this.loadPage();
  }

  protected onSortChange(sort: Sort): void {
    this.sortActive = sort.active;
    this.sortDirection = sort.direction as 'asc' | 'desc';
    this.currentPage = 0;
    this.loadPage();
  }

  private loadPage(): void {
    this.loading.set(true);
    const search = this.searchValue().trim() || undefined;
    const isCurrent = this.isCurrentFilter() ?? undefined;
    this.academicYearService.getPage({
      search,
      isCurrent,
      page: this.currentPage,
      size: this.currentPageSize,
      sort: this.sortMap[this.sortActive] ?? this.sortActive,
      direction: this.sortDirection,
    }).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        if (this._paginator) {
          this._paginator.length = page.totalElements;
          this._paginator.pageIndex = page.number;
        }
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load academic years');
        this.loading.set(false);
      },
    });
  }

  protected editAcademicYear(academicYear: AcademicYear): void {
    void this.router.navigate(['/academic-years', academicYear.id, 'edit']);
  }

  protected deleteAcademicYear(academicYear: AcademicYear): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Academic Year',
        message: `Are you sure you want to delete "${academicYear.name}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.performDelete(academicYear);
    });
  }

  private performDelete(academicYear: AcademicYear): void {
    this.loading.set(true);
    this.academicYearService.deleteAcademicYear(academicYear.id).subscribe({
      next: () => {
        this.toast.success('Academic year deleted successfully');
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? 'Failed to delete academic year');
        this.loading.set(false);
      },
    });
  }
}
