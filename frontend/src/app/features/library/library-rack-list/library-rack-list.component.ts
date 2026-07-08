import { Component, computed, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { LibraryService } from '../library.service';
import { LibraryRack } from '../library.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { PermissionService } from '../../../core/permissions/permission.service';

@Component({
  selector: 'app-library-rack-list',
  standalone: true,
  imports: [
    RouterLink, FormsModule,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatDialogModule, MatIconModule, MatTooltipModule,
    CmsEmptyStateComponent, CmsRowActionButtonComponent, CmsStatusBadgeComponent,
  ],
  templateUrl: './library-rack-list.component.html',
  styleUrl: './library-rack-list.component.scss',
})
export class LibraryRackListComponent implements OnInit, OnDestroy {
  private readonly libraryService = inject(LibraryService);
  private readonly router         = inject(Router);
  private readonly toast          = inject(ToastService);
  private readonly dialog         = inject(MatDialog);
  protected readonly permissions  = inject(PermissionService);

  private readonly destroy$      = new Subject<void>();
  private readonly searchSubject = new Subject<string>();
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

  protected readonly displayedColumns = ['name', 'code', 'description', 'isActive', 'actions'];
  protected readonly dataSource   = new MatTableDataSource<LibraryRack>([]);
  protected readonly loading      = signal(false);
  protected readonly searchValue  = signal('');
  protected readonly canManage    = computed(() => this.permissions.hasAny('LIBRARY_SHELF_MANAGE'));

  protected totalElements   = 0;
  protected currentPage     = 0;
  protected currentPageSize = 25;
  protected sortActive      = 'name';
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

  protected clearFilter(): void {
    this.searchValue.set('');
    this.searchSubject.next('');
  }

  protected onSortChange(sort: Sort): void {
    this.sortActive     = sort.active;
    this.sortDirection  = (sort.direction || 'asc') as 'asc' | 'desc';
    this.currentPage    = 0;
    this.loadPage();
  }

  protected editRack(rack: LibraryRack): void {
    void this.router.navigate(['/library/racks', rack.id, 'edit']);
  }

  protected viewShelves(rack: LibraryRack): void {
    void this.router.navigate(['/library/racks', rack.id, 'shelves']);
  }

  protected toggleRackStatus(rack: LibraryRack): void {
    const nextAction = rack.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${nextAction} Rack`,
        message: `${nextAction} "${rack.name}" (${rack.code})?`,
        confirmText: nextAction,
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(confirmed => { if (confirmed) this.performToggle(rack); });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else if (this.canManage()) {
      void this.router.navigate(['/library/racks/new']);
    }
  }

  private performToggle(rack: LibraryRack): void {
    this.libraryService.updateRackStatus(rack.id, !rack.isActive).subscribe({
      next: () => {
        this.toast.success(`Rack ${rack.isActive ? 'deactivated' : 'activated'} successfully`);
        this.loadPage();
      },
      error: (err) => {
        this.toast.error(err?.error?.message ?? `Failed to ${rack.isActive ? 'deactivate' : 'activate'} rack`);
      },
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    this.libraryService.getRacksPage({
      search: this.searchValue().trim() || undefined,
      page: this.currentPage, size: this.currentPageSize,
      sort: this.sortActive, direction: this.sortDirection,
    }).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        if (this._paginator) { this._paginator.length = page.totalElements; this._paginator.pageIndex = page.number; }
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load racks'); this.loading.set(false); },
    });
  }
}
