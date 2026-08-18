import { Component, inject, OnInit, OnDestroy, AfterViewInit, signal, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { InrPipe } from '../../../shared/pipes/inr.pipe';
import { ReferralTypeService } from '../referral-type.service';
import { ReferralType } from '../referral-type.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { REFERRAL_TYPE_LIST_TOUR, REFERRAL_TYPE_LIST_FLOW_MAP } from '../../../shared/tour/tours/referral-type.tours';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent, CmsIconToggleStatusComponent } from '../../../shared/icons';

@Component({
  selector: 'app-referral-type-list',
  standalone: true,
  imports: [
    InrPipe,
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
    CmsViewToggleComponent,
    CmsStatusBadgeComponent,
    CmsTourButtonComponent,
    CmsRowActionButtonComponent,
      CmsIconEditComponent,
      CmsIconToggleStatusComponent,
  ],
  templateUrl: './referral-type-list.component.html',
  styleUrl: './referral-type-list.component.scss',
})
export class ReferralTypeListComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly referralTypeService = inject(ReferralTypeService);
  private readonly router = inject(Router);
  private readonly route  = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(MatDialog);
  private readonly tourService = inject(TourService);

  private readonly VIEW_MODE_KEY = 'referral-type-view-mode';
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

  protected readonly displayedColumns = ['name', 'code', 'hasCommission', 'commissionAmount', 'isActive', 'actions'];
  protected readonly dataSource = new MatTableDataSource<ReferralType>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;
  protected sortActive = 'name';
  protected sortDirection: 'asc' | 'desc' = 'asc';
  private readonly sortMap: Record<string, string> = {
    name: 'name', code: 'code', isActive: 'isActive',
  };

  ngOnInit(): void {
    this.tourService.register('referral-type-list', REFERRAL_TYPE_LIST_TOUR);
    this.tourService.registerFlowMap('referral-type-list', REFERRAL_TYPE_LIST_FLOW_MAP);
    const snap = this.route.snapshot.queryParams;
    if (snap['sortField']) this.sortActive    = snap['sortField'];
    if (snap['sortDir'])   this.sortDirection = snap['sortDir'] as 'asc' | 'desc';
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

  ngAfterViewInit(): void {}

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
    this.sortActive    = sort.active;
    this.sortDirection = (sort.direction || 'asc') as 'asc' | 'desc';
    this.currentPage   = 0;
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { sortField: sort.active, sortDir: this.sortDirection },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
    this.loadPage();
  }

  protected edit(item: ReferralType): void {
    void this.router.navigate(['/referral-types', item.id, 'edit']);
  }

  protected toggleStatus(item: ReferralType): void {
    const nextAction = item.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${nextAction} Referral Type`,
        message: `${nextAction} "${item.name}"?`,
        confirmText: nextAction,
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed) => {
      if (confirmed) this.doToggle(item);
    });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      void this.router.navigate(['/referral-types/new']);
    }
  }

  private loadViewMode(): 'card' | 'table' {
    return localStorage.getItem(this.VIEW_MODE_KEY) === 'table' ? 'table' : 'card';
  }

  private doToggle(item: ReferralType): void {
    this.loading.set(true);
    const request$ = item.isActive
      ? this.referralTypeService.deactivateReferralType(item.id)
      : this.referralTypeService.reactivateReferralType(item.id);
    request$.subscribe({
      next: () => {
        this.toast.success(`Referral type ${item.isActive ? 'deactivated' : 'activated'} successfully`);
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(
          err?.error?.message ?? `Failed to ${item.isActive ? 'deactivate' : 'activate'} referral type`,
        );
        this.loading.set(false);
      },
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    const search = this.searchValue().trim() || undefined;
    this.referralTypeService.getPage({ search, page: this.currentPage, size: this.currentPageSize, sort: this.sortMap[this.sortActive] ?? this.sortActive, direction: this.sortDirection }).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        if (this._paginator) {
          this._paginator.length = page.totalElements;
          this._paginator.pageIndex = page.number;
        }
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load referral types'); this.loading.set(false); },
    });
  }
}
