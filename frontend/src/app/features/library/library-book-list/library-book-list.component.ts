import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { FormsModule } from '@angular/forms';
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
    RouterLink,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatSelectModule,
    MatInputModule,
    CmsRowActionButtonComponent,
    MatFormFieldModule,
    CmsEmptyStateComponent,
  ],
  templateUrl: './library-book-list.component.html',
  styleUrl: './library-book-list.component.scss',
})
export class LibraryBookListComponent implements OnInit {
  private readonly libraryService  = inject(LibraryService);
  private readonly router          = inject(Router);
  private readonly toast           = inject(ToastService);
  private readonly dialog          = inject(MatDialog);
  protected readonly permissions   = inject(PermissionService);

  @ViewChild(MatPaginator) set paginator(v: MatPaginator) { if (v) this.dataSource.paginator = v; }
  @ViewChild(MatSort)      set sort(v: MatSort)           { if (v) this.dataSource.sort = v; }

  protected readonly displayedColumns = [
    'accessionNumber', 'title', 'authors', 'publisher',
    'shelfLocation', 'callNumber', 'status', 'actions',
  ];
  protected readonly dataSource  = new MatTableDataSource<LibraryBook>([]);
  protected readonly loading     = signal(false);
  protected readonly searchValue = signal('');
  protected readonly statusFilter = signal<BookStatus | null>(null);
  protected readonly categoryFilter = signal<string | null>(null);

  private readonly allBooks = signal<LibraryBook[]>([]);
  protected readonly statusOptions = BOOK_STATUS_OPTIONS;
  protected readonly categoryOptions = SUBJECT_CATEGORY_OPTIONS;

  protected readonly filteredBooks = computed(() => {
    const q      = this.searchValue().trim().toLowerCase();
    const status = this.statusFilter();
    const cat    = this.categoryFilter();

    return this.allBooks().filter(b => {
      if (status && b.status !== status) return false;
      if (cat && b.subjectCategory !== cat) return false;
      if (q && !(
        b.accessionNumber.toLowerCase().includes(q) ||
        b.title.toLowerCase().includes(q) ||
        b.authors.toLowerCase().includes(q) ||
        (b.publisher ?? '').toLowerCase().includes(q) ||
        (b.callNumber ?? '').toLowerCase().includes(q)
      )) return false;
      return true;
    });
  });

  protected readonly totalCount     = computed(() => this.allBooks().length);
  protected readonly availableCount = computed(() => this.allBooks().filter(b => b.status === 'AVAILABLE').length);
  protected readonly issuedCount    = computed(() => this.allBooks().filter(b => b.status === 'ISSUED').length);

  protected readonly hasActiveFilters = computed(() =>
    this.statusFilter() !== null || this.categoryFilter() !== null || this.searchValue().length > 0,
  );

  protected readonly canManage = computed(() =>
    this.permissions.hasAny('LIBRARY_CATALOGUE_MANAGE'),
  );

  ngOnInit(): void {
    this.dataSource.sortingDataAccessor = (item, prop) =>
      String((item as unknown as Record<string, unknown>)[prop] ?? '').toLowerCase();
    this.loadBooks();
  }

  protected applyFilter(event: Event): void {
    this.searchValue.set((event.target as HTMLInputElement).value);
    this.syncTable();
  }

  protected clearFilters(): void {
    this.searchValue.set('');
    this.statusFilter.set(null);
    this.categoryFilter.set(null);
    this.syncTable();
  }

  protected editBook(book: LibraryBook): void {
    void this.router.navigate(['/library/books', book.id, 'edit']);
  }

  protected deleteBook(book: LibraryBook): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Book',
        message: `Delete "${book.title}" (${book.accessionNumber})? This cannot be undone.`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) this.performDelete(book);
    });
  }

  protected statusClass(status: BookStatus): string {
    return BOOK_STATUS_OPTIONS.find(o => o.value === status)?.colorClass ?? '';
  }

  protected statusLabel(status: BookStatus): string {
    return BOOK_STATUS_OPTIONS.find(o => o.value === status)?.label ?? status;
  }

  protected handleEmptyAction(): void {
    if (this.hasActiveFilters()) this.clearFilters();
    else void this.router.navigate(['/library/books/new']);
  }

  private loadBooks(): void {
    this.loading.set(true);
    this.libraryService.getAll().subscribe({
      next: books => {
        this.allBooks.set(books);
        this.syncTable();
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load book catalogue');
        this.loading.set(false);
      },
    });
  }

  private performDelete(book: LibraryBook): void {
    this.libraryService.delete(book.id).subscribe({
      next: () => {
        this.toast.success('Book deleted successfully');
        this.loadBooks();
      },
      error: (err) => {
        const msg = err?.error?.message ?? 'Failed to delete book';
        this.toast.error(msg);
      },
    });
  }

  protected syncTable(): void {
    this.dataSource.data = this.filteredBooks();
    this.dataSource.paginator?.firstPage();
  }
}
