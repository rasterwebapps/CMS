import { Component, inject, OnInit, OnDestroy, signal, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { ClinicalVenueService } from '../clinical-venue.service';
import { ClinicalVenue } from '../clinical-venue.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsViewToggleComponent } from '../../../shared/view-toggle/view-toggle.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent, CmsIconToggleStatusComponent } from '../../../shared/icons';

@Component({
  selector: 'app-clinical-venue-list',
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
  ],
  templateUrl: './clinical-venue-list.component.html',
  styleUrl: './clinical-venue-list.component.scss',
})
export class ClinicalVenueListComponent implements OnInit, OnDestroy {
  private readonly clinicalVenueService = inject(ClinicalVenueService);
  private readonly router               = inject(Router);
  private readonly route                = inject(ActivatedRoute);
  private readonly toast                = inject(ToastService);
  private readonly dialog               = inject(MatDialog);

  private readonly VIEW_MODE_KEY = 'clinical-venue-view-mode';
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

  protected readonly displayedColumns = ['name', 'hospitalName', 'department', 'isActive', 'actions'];
  protected readonly dataSource = new MatTableDataSource<ClinicalVenue>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');
  protected readonly viewMode = signal<'card' | 'table'>(this.loadViewMode());

  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;
  protected sortActive = 'name';
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

  protected editVenue(item: ClinicalVenue): void {
    void this.router.navigate(['/clinical-venues', item.id, 'edit']);
  }

  protected toggleVenueStatus(item: ClinicalVenue): void {
    const nextAction = item.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${nextAction} Clinical Venue`,
        message: `${nextAction} "${item.name}"?`,
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
      void this.router.navigate(['/clinical-venues/new']);
    }
  }

  private loadViewMode(): 'card' | 'table' {
    return localStorage.getItem(this.VIEW_MODE_KEY) === 'table' ? 'table' : 'card';
  }

  private performToggle(item: ClinicalVenue): void {
    this.loading.set(true);
    this.clinicalVenueService.updateStatus(item.id, { isActive: !item.isActive }).subscribe({
      next: () => {
        this.toast.success(`Clinical venue ${item.isActive ? 'deactivated' : 'activated'} successfully`);
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(
          err?.error?.message ?? `Failed to ${item.isActive ? 'deactivate' : 'activate'} clinical venue`,
        );
        this.loading.set(false);
      },
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    const search = this.searchValue().trim() || undefined;
    this.clinicalVenueService.getPage({ search, page: this.currentPage, size: this.currentPageSize, sort: this.sortActive, direction: this.sortDirection }).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        if (this._paginator) {
          this._paginator.length = page.totalElements;
          this._paginator.pageIndex = page.number;
        }
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load clinical venues'); this.loading.set(false); },
    });
  }
}
