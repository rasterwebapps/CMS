import { Component, computed, inject, OnInit, OnDestroy, signal, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { HolidayTemplateService } from '../holiday-template.service';
import { HolidayTemplate } from '../holiday-template.model';
import { formatRecurrenceSummary } from '../holiday-template-summary.util';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { PermissionService } from '../../../core/permissions/permission.service';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent, CmsIconToggleStatusComponent } from '../../../shared/icons';
import { TourService } from '../../../shared/tour/tour.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { HOLIDAY_TEMPLATE_LIST_TOUR, HOLIDAY_TEMPLATE_LIST_FLOW_MAP } from '../../../shared/tour/tours/preferences-remainder.tours';

@Component({
  selector: 'app-holiday-template-list',
  standalone: true,
  imports: [
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
    CmsViewToggleComponent,
    CmsStatusBadgeComponent,
    CmsRowActionButtonComponent,
    CmsIconEditComponent,
    CmsIconToggleStatusComponent,
    CmsTourButtonComponent,
  ],
  templateUrl: './holiday-template-list.component.html',
  styleUrl: './holiday-template-list.component.scss',
})
export class HolidayTemplateListComponent implements OnInit, OnDestroy {
  private readonly holidayTemplateService = inject(HolidayTemplateService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly permissionService = inject(PermissionService);
  private readonly tourService = inject(TourService);

  private readonly VIEW_MODE_KEY = 'holiday-template-view-mode';
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

  protected readonly canManage = computed(() => this.permissionService.has('HOLIDAY_TEMPLATE_MANAGE'));
  protected readonly displayedColumns = computed(() =>
    this.canManage()
      ? ['name', 'recurrence', 'holidayCategory', 'isActive', 'actions']
      : ['name', 'recurrence', 'holidayCategory', 'isActive'],
  );
  protected readonly dataSource = new MatTableDataSource<HolidayTemplate>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;
  protected sortActive = 'name';
  protected sortDirection: 'asc' | 'desc' = 'asc';

  protected readonly recurrenceSummary = formatRecurrenceSummary;

  ngOnInit(): void {
    this.tourService.register('holiday-template-list', HOLIDAY_TEMPLATE_LIST_TOUR);
    this.tourService.registerFlowMap('holiday-template-list', HOLIDAY_TEMPLATE_LIST_FLOW_MAP);

    const snap = this.route.snapshot.queryParams;
    if (snap['sortField']) this.sortActive = snap['sortField'];
    if (snap['sortDir']) this.sortDirection = snap['sortDir'] as 'asc' | 'desc';
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(() => {
      this.currentPage = 0;
      this.loadPage();
    });
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

  protected applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.searchSubject.next(value);
  }

  protected clearFilter(): void {
    this.searchValue.set('');
    this.searchSubject.next('');
  }

  protected onSortChange(sort: Sort): void {
    this.sortActive = sort.active;
    this.sortDirection = (sort.direction || 'asc') as 'asc' | 'desc';
    this.currentPage = 0;
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { sortField: sort.active, sortDir: this.sortDirection },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
    this.loadPage();
  }

  protected edit(item: HolidayTemplate): void {
    if (!this.canManage()) {
      this.toast.error('You do not have permission to edit holiday templates');
      return;
    }
    void this.router.navigate(['/holiday-templates', item.id, 'edit']);
  }

  protected toggleStatus(item: HolidayTemplate): void {
    if (!this.canManage()) {
      this.toast.error('You do not have permission to update holiday templates');
      return;
    }
    const nextAction = item.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${nextAction} Holiday Template`,
        message: `${nextAction} "${item.name}"? ${item.isActive ? 'It will stop seeding into any newly-created academic year.' : ''}`,
        confirmText: nextAction,
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.performToggle(item);
    });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      if (!this.canManage()) {
        this.toast.error('You do not have permission to add holiday templates');
        return;
      }
      void this.router.navigate(['/holiday-templates/new']);
    }
  }

  private loadViewMode(): 'card' | 'table' {
    return localStorage.getItem(this.VIEW_MODE_KEY) === 'table' ? 'table' : 'card';
  }

  /** No dedicated /status endpoint exists on the backend for this master -- toggling status is
   *  just a full update with every existing field carried over and isActive flipped. */
  private performToggle(item: HolidayTemplate): void {
    this.loading.set(true);
    this.holidayTemplateService.update(item.id, {
      name: item.name,
      recurrenceType: item.recurrenceType,
      holidayCategory: item.holidayCategory,
      description: item.description ?? undefined,
      durationDays: item.durationDays,
      month: item.month,
      dayOfMonth: item.dayOfMonth,
      weekOfMonth: item.weekOfMonth,
      dayOfWeek: item.dayOfWeek,
      isActive: !item.isActive,
    }).subscribe({
      next: () => {
        this.toast.success(`Holiday template ${item.isActive ? 'deactivated' : 'activated'} successfully`);
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(
          err?.error?.message ?? `Failed to ${item.isActive ? 'deactivate' : 'activate'} holiday template`,
        );
        this.loading.set(false);
      },
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    const search = this.searchValue().trim() || undefined;
    this.holidayTemplateService.getPage({
      search, page: this.currentPage, size: this.currentPageSize,
      sort: this.sortActive, direction: this.sortDirection,
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
      error: () => { this.toast.error('Failed to load holiday templates'); this.loading.set(false); },
    });
  }
}
