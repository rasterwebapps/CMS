import { Component, computed, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { LibraryService } from '../library.service';
import { LibraryBook, BookStatus, BOOK_STATUS_OPTIONS, SUBJECT_CATEGORY_OPTIONS } from '../library.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { PermissionService } from '../../../core/permissions/permission.service';

@Component({
  selector: 'app-library-book-list',
  standalone: true,
  imports: [
    RouterLink, FormsModule,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatDialogModule, MatButtonModule, MatIconModule, MatTooltipModule,
    MatSelectModule, MatInputModule, MatFormFieldModule,
    CmsRowActionButtonComponent, CmsEmptyStateComponent,
  ],
  templateUrl: './library-book-list.component.html',
  styleUrl: './library-book-list.component.scss',
})
export class LibraryBookListComponent implements OnInit, OnDestroy {
  private readonly libraryService = inject(LibraryService);
  private readonly router         = inject(Router);
  private readonly toast          = inject(ToastService);
  private readonly dialog         = inject(MatDialog);
  protected readonly permissions  = inject(PermissionService);

  private readonly destroy$     = new Subject<void>();
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

  protected readonly displayedColumns = [
    'accessionNumber', 'title', 'authors', 'publisher',
    'shelfLocation', 'callNumber', 'status', 'actions',
  ];
  protected readonly dataSource    = new MatTableDataSource<LibraryBook>([]);
  protected readonly loading       = signal(false);
  protected readonly searchValue   = signal('');
  protected readonly statusFilter  = signal<BookStatus | null>(null);
  protected readonly categoryFilter = signal<string | null>(null);
  protected readonly statusOptions  = BOOK_STATUS_OPTIONS;
  protected readonly categoryOptions = SUBJECT_CATEGORY_OPTIONS;
  protected readonly canManage      = computed(() => this.permissions.hasAny('LIBRARY_CATALOGUE_MANAGE'));
  protected readonly hasActiveFilters = computed(() =>
    this.statusFilter() !== null || this.categoryFilter() !== null || this.searchValue().length > 0);

  protected totalElements  = 0;
  protected currentPage    = 0;
  protected currentPageSize = 25;
  protected sortActive     = 'title';
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
    this.searchValue.set(''); this.statusFilter.set(null); this.categoryFilter.set(null);
    this.currentPage = 0; this.loadPage();
  }

  protected onSortChange(sort: Sort): void {
    this.sortActive = sort.active;
    this.sortDirection = sort.direction as 'asc' | 'desc';
    this.currentPage = 0;
    this.loadPage();
  }

  protected statusClass(status: BookStatus): string {
    return BOOK_STATUS_OPTIONS.find(o => o.value === status)?.colorClass ?? '';
  }

  protected statusLabel(status: BookStatus): string {
    return BOOK_STATUS_OPTIONS.find(o => o.value === status)?.label ?? status;
  }

  protected editBook(book: LibraryBook): void {
    void this.router.navigate(['/library/books', book.id, 'edit']);
  }

  protected deleteBook(book: LibraryBook): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete Book', message: `Delete "${book.title}" (${book.accessionNumber})? This cannot be undone.`, confirmText: 'Delete', cancelText: 'Cancel' },
    }).afterClosed().subscribe(confirmed => { if (confirmed) this.performDelete(book); });
  }

  protected handleEmptyAction(): void {
    if (this.hasActiveFilters()) this.clearFilters();
    else void this.router.navigate(['/library/books/new']);
  }

  private loadPage(): void {
    this.loading.set(true);
    this.libraryService.getBooksPage({
      search: this.searchValue() || undefined,
      status: this.statusFilter(),
      category: this.categoryFilter(),
      page: this.currentPage, size: this.currentPageSize,
      sort: this.sortActive, direction: this.sortDirection,
    }).subscribe({
      next: page => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        if (this._paginator) { this._paginator.length = page.totalElements; this._paginator.pageIndex = page.number; }
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load book catalogue'); this.loading.set(false); },
    });
  }

  private performDelete(book: LibraryBook): void {
    this.libraryService.delete(book.id).subscribe({
      next: () => { this.toast.success('Book deleted successfully'); this.loadPage(); },
      error: err => this.toast.error(err?.error?.message ?? 'Failed to delete book'),
    });
  }
}
