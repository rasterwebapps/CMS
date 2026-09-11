import { Component, inject, OnInit, OnDestroy, ViewChild, signal } from '@angular/core';
import { DecimalPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { CurrencyExchangeRateService } from '../currency-exchange-rate.service';
import { CurrencyExchangeRate } from '../currency-exchange-rate.model';
import { ConfirmDialogComponent } from '../../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../../../core/toast/toast.service';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent, CmsIconToggleStatusComponent } from '../../../../../shared/icons';

@Component({
  selector: 'app-currency-exchange-rate-list',
  standalone: true,
  imports: [
    RouterLink,
    DecimalPipe,
    DatePipe,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
    CmsStatusBadgeComponent,
    CmsRowActionButtonComponent,
    CmsIconEditComponent,
    CmsIconToggleStatusComponent,
  ],
  templateUrl: './currency-exchange-rate-list.component.html',
  styleUrl: './currency-exchange-rate-list.component.scss',
})
export class CurrencyExchangeRateListComponent implements OnInit, OnDestroy {
  private readonly rateService = inject(CurrencyExchangeRateService);
  private readonly router      = inject(Router);
  private readonly route       = inject(ActivatedRoute);
  private readonly toast       = inject(ToastService);
  private readonly dialog      = inject(MatDialog);

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

  protected readonly displayedColumns = ['currencyCode', 'rateToBase', 'effectiveDate', 'isActive', 'actions'];
  protected readonly dataSource = new MatTableDataSource<CurrencyExchangeRate>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;
  protected sortActive = 'currencyCode';
  protected sortDirection: 'asc' | 'desc' = 'asc';

  ngOnInit(): void {
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

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this._paginatorSub?.unsubscribe();
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

  protected editRate(item: CurrencyExchangeRate): void {
    void this.router.navigate(['/inventory/procurement/currency-exchange-rates', item.id, 'edit']);
  }

  protected toggleRateStatus(item: CurrencyExchangeRate): void {
    const nextAction = item.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${nextAction} Exchange Rate`,
        message: `${nextAction} the ${item.currencyCode} rate effective ${item.effectiveDate}?`,
        confirmText: nextAction,
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) this.performToggle(item);
    });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      void this.router.navigate(['/inventory/procurement/currency-exchange-rates/new']);
    }
  }

  private performToggle(item: CurrencyExchangeRate): void {
    this.loading.set(true);
    this.rateService.updateStatus(item.id, { isActive: !item.isActive }).subscribe({
      next: () => {
        this.toast.success(`Exchange rate ${item.isActive ? 'deactivated' : 'activated'} successfully`);
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? `Failed to ${item.isActive ? 'deactivate' : 'activate'} exchange rate`);
        this.loading.set(false);
      },
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    const search = this.searchValue().trim() || undefined;
    this.rateService.getPage({
      search,
      page: this.currentPage,
      size: this.currentPageSize,
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
      error: () => { this.toast.error('Failed to load exchange rates'); this.loading.set(false); },
    });
  }
}
