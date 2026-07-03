import { Component, computed, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { LibraryService } from '../library.service';
import { LibraryPeriodical, JournalType, SubscriptionStatus, JOURNAL_TYPE_OPTIONS, SUBSCRIPTION_STATUS_OPTIONS } from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { PermissionService } from '../../../core/permissions/permission.service';

@Component({
  selector: 'app-library-periodical-list',
  standalone: true,
  imports: [
    RouterLink, DatePipe, FormsModule,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatDialogModule, MatButtonModule, MatIconModule, MatTooltipModule,
    CmsEmptyStateComponent, CmsRowActionButtonComponent,
    CmsTypeBadgeComponent, CmsStatusBadgeComponent,
  ],
  templateUrl: './library-periodical-list.component.html',
  styleUrl:    './library-periodical-list.component.scss',
})
export class LibraryPeriodicalListComponent implements OnInit, OnDestroy {
  private readonly libraryService = inject(LibraryService);
  private readonly router         = inject(Router);
  private readonly toast          = inject(ToastService);
  private readonly dialog         = inject(MatDialog);
  protected readonly permissions  = inject(PermissionService);

  private readonly destroy$      = new Subject<void>();
  private readonly searchSubject  = new Subject<string>();
  private _paginator?: MatPaginator;
  private _paginatorSub?: Subscription;

  @ViewChild(MatPaginator) set paginatorRef(p: MatPaginator | undefined) {
    if (!p || p === this._paginator) return;
    this._paginatorSub?.unsubscribe();
    this._paginator = p;
    p.pageIndex = this.currentPage;
    p.pageSize  = this.currentPageSize;
    this._paginatorSub = p.page.pipe(takeUntil(this.destroy$)).subscribe((e: PageEvent) => {
      this.currentPage = e.pageIndex;
      this.currentPageSize = e.pageSize;
      this.loadPage();
    });
  }

  protected readonly displayedColumns = [
    'journalName', 'journalType', 'volumeIssue', 'year',
    'copiesCount', 'subscriptionStatus', 'receivedDate', 'actions',
  ];
  protected readonly dataSource    = new MatTableDataSource<LibraryPeriodical>([]);
  protected readonly loading       = signal(false);
  protected readonly searchValue   = signal('');
  protected readonly typeFilter    = signal<JournalType | null>(null);
  protected readonly statusFilter  = signal<SubscriptionStatus | null>(null);
  protected readonly typeOptions   = JOURNAL_TYPE_OPTIONS;
  protected readonly statusOptions = SUBSCRIPTION_STATUS_OPTIONS;
  protected readonly canManage     = computed(() => this.permissions.hasAny('LIBRARY_PERIODICAL_MANAGE'));
  protected readonly hasActiveFilters = computed(() =>
    this.typeFilter() !== null || this.statusFilter() !== null || this.searchValue().length > 0);

  protected totalElements  = 0;
  protected currentPage    = 0;
  protected currentPageSize = 25;
  protected sortActive     = 'journalName';
  protected sortDirection: 'asc' | 'desc' = 'asc';

  ngOnInit(): void {
    this.searchSubject.pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => { this.currentPage = 0; this.loadPage(); });
    this.loadPage();
  }

  ngOnDestroy(): void {
    this.destroy$.next(); this.destroy$.complete();
    this._paginatorSub?.unsubscribe();
  }

  protected applyFilter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue.set(value);
    this.searchSubject.next(value);
  }

  protected onFilterChange(): void { this.currentPage = 0; this.loadPage(); }

  protected clearFilters(): void {
    this.searchValue.set(''); this.typeFilter.set(null); this.statusFilter.set(null);
    this.currentPage = 0; this.loadPage();
  }

  protected onSortChange(sort: Sort): void {
    this.sortActive = sort.active;
    this.sortDirection = sort.direction as 'asc' | 'desc';
    this.currentPage = 0; this.loadPage();
  }

  protected editItem(p: LibraryPeriodical): void {
    void this.router.navigate(['/library/periodicals', p.id, 'edit']);
  }

  protected deleteItem(p: LibraryPeriodical): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete Periodical', message: `Delete "${p.journalName}"${p.volumeNumber ? ' Vol. ' + p.volumeNumber : ''}? This cannot be undone.`, confirmText: 'Delete', cancelText: 'Cancel' },
    }).afterClosed().subscribe(confirmed => { if (confirmed) this.performDelete(p); });
  }

  protected handleEmptyAction(): void {
    if (this.hasActiveFilters()) this.clearFilters();
    else void this.router.navigate(['/library/periodicals/new']);
  }

  private loadPage(): void {
    this.loading.set(true);
    this.libraryService.getPeriodicalsPage({
      search: this.searchValue() || undefined,
      subscriptionStatus: this.statusFilter(),
      journalType: this.typeFilter(),
      page: this.currentPage, size: this.currentPageSize,
      sort: this.sortActive, direction: this.sortDirection,
    }).subscribe({
      next: page => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        if (this._paginator) { this._paginator.length = page.totalElements; this._paginator.pageIndex = page.number; }
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load periodicals'); this.loading.set(false); },
    });
  }

  private performDelete(p: LibraryPeriodical): void {
    this.libraryService.deletePeriodical(p.id).subscribe({
      next: () => { this.toast.success('Periodical entry deleted'); this.loadPage(); },
      error: err => this.toast.error(err?.error?.message ?? 'Failed to delete'),
    });
  }
}
