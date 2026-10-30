import { Component, computed, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
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
import { LibrarySubjectCategory } from '../library.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { LibrarySubjectCategoryDialogComponent } from '../library-subject-category-dialog/library-subject-category-dialog.component';

@Component({
  selector: 'app-library-subject-category-list',
  standalone: true,
  imports: [
    FormsModule,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatDialogModule, MatIconModule, MatTooltipModule,
    CmsEmptyStateComponent, CmsRowActionButtonComponent, CmsStatusBadgeComponent,
  ],
  templateUrl: './library-subject-category-list.component.html',
  styleUrl: './library-subject-category-list.component.scss',
})
export class LibrarySubjectCategoryListComponent implements OnInit, OnDestroy {
  private readonly libraryService = inject(LibraryService);
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
  protected readonly dataSource   = new MatTableDataSource<LibrarySubjectCategory>([]);
  protected readonly loading      = signal(false);
  protected readonly searchValue  = signal('');
  protected readonly canManage    = computed(() =>
    this.permissions.hasAny('LIBRARY_SUBJECT_CATEGORY_MANAGE', 'LIBRARY_CATALOGUE_MANAGE'));

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

  protected openAddDialog(): void {
    this.dialog.open(LibrarySubjectCategoryDialogComponent, {
      width: '460px',
    }).afterClosed().subscribe((result) => {
      if (result) this.loadPage();
    });
  }

  protected editCategory(category: LibrarySubjectCategory): void {
    this.dialog.open(LibrarySubjectCategoryDialogComponent, {
      width: '460px',
      data: { category },
    }).afterClosed().subscribe((result) => {
      if (result) this.loadPage();
    });
  }

  protected toggleCategoryStatus(category: LibrarySubjectCategory): void {
    const nextAction = category.isActive ? 'Deactivate' : 'Activate';
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${nextAction} Subject Category`,
        message: `${nextAction} "${category.name}" (${category.code})?`,
        confirmText: nextAction,
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) this.performToggle(category);
    });
  }

  protected deleteCategory(category: LibrarySubjectCategory): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Subject Category',
        message: `Are you sure you want to delete category "${category.name}"? This action cannot be undone.`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
        isDestructive: true,
      },
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.libraryService.deleteSubjectCategory(category.id).subscribe({
          next: () => {
            this.toast.success(`Category "${category.name}" deleted successfully`);
            this.loadPage();
          },
          error: (err) => {
            const msg = err.error?.message || err.error?.detail || err.message || 'Cannot delete category';
            this.toast.error(msg);
          },
        });
      }
    });
  }

  protected handleEmptyAction(): void {
    if (this.searchValue()) {
      this.clearFilter();
    } else if (this.canManage()) {
      this.openAddDialog();
    }
  }

  private performToggle(category: LibrarySubjectCategory): void {
    this.libraryService.updateSubjectCategoryStatus(category.id, !category.isActive).subscribe({
      next: () => {
        this.toast.success(`Category "${category.name}" ${category.isActive ? 'deactivated' : 'activated'} successfully`);
        this.loadPage();
      },
      error: (err) => {
        const msg = err.error?.message || 'Failed to update category status';
        this.toast.error(msg);
      },
    });
  }

  protected loadPage(): void {
    this.loading.set(true);
    this.libraryService.getSubjectCategoriesPage({
      search: this.searchValue() || undefined,
      page: this.currentPage,
      size: this.currentPageSize,
      sort: this.sortActive,
      direction: this.sortDirection,
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements   = page.totalElements;
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Failed to load subject categories');
      },
    });
  }
}
