import { Component, inject, OnInit, OnDestroy, ViewChild, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { ServiceTicketCategoryService } from '../category.service';
import { ServiceTicketCategory } from '../category.model';
import { ConfirmDialogComponent } from '../../../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../../../shared/empty-state/empty-state.component';
import { CmsStatusBadgeComponent } from '../../../../../shared/status-badge/status-badge.component';
import { CmsRowActionButtonComponent } from '../../../../../shared/row-action-button/row-action-button.component';
import { CmsIconEditComponent, CmsIconToggleStatusComponent } from '../../../../../shared/icons';
import { ToastService } from '../../../../../core/toast/toast.service';

@Component({
  selector: 'app-service-ticket-category-list',
  standalone: true,
  imports: [
    RouterLink,
    FormsModule,
    MatDialogModule,
    MatTableModule,
    MatPaginatorModule,
    CmsEmptyStateComponent,
    CmsStatusBadgeComponent,
    CmsRowActionButtonComponent,
    CmsIconEditComponent,
    CmsIconToggleStatusComponent,
  ],
  templateUrl: './category-list.component.html',
  styleUrl: './category-list.component.scss',
})
export class ServiceTicketCategoryListComponent implements OnInit, OnDestroy {
  private readonly categoryService = inject(ServiceTicketCategoryService);
  private readonly router          = inject(Router);
  private readonly dialog          = inject(MatDialog);
  private readonly toast           = inject(ToastService);

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

  protected readonly displayedColumns = ['name', 'description', 'isActive', 'actions'];
  protected readonly dataSource = new MatTableDataSource<ServiceTicketCategory>([]);
  protected readonly loading = signal(false);
  protected readonly searchValue = signal('');

  protected totalElements = 0;
  protected currentPage = 0;
  protected currentPageSize = 25;

  ngOnInit(): void {
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

  protected editCategory(item: ServiceTicketCategory): void {
    void this.router.navigate(['/inventory/ticket/categories', item.id, 'edit']);
  }

  protected toggleStatus(item: ServiceTicketCategory): void {
    const nextAction = item.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: `${nextAction} Category`, message: `${nextAction} "${item.name}"?`, confirmText: nextAction, cancelText: 'Cancel' },
    }).afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.loading.set(true);
      this.categoryService.updateStatus(item.id, { isActive: !item.isActive }).subscribe({
        next: () => { this.toast.success(`Category ${item.isActive ? 'deactivated' : 'activated'}`); this.loadPage(); },
        error: (err) => { this.toast.error(err?.error?.message ?? 'Failed to update category status'); this.loading.set(false); },
      });
    });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else {
      void this.router.navigate(['/inventory/ticket/categories/new']);
    }
  }

  private loadPage(): void {
    this.loading.set(true);
    this.categoryService.getPage({
      search: this.searchValue().trim() || undefined,
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
      error: () => { this.toast.error('Failed to load categories'); this.loading.set(false); },
    });
  }
}
