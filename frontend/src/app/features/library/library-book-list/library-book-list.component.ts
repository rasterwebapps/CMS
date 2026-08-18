import { Component, computed, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SelectionModel } from '@angular/cdk/collections';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { LibraryService } from '../library.service';
import { LibraryPrintTransportService } from '../library-print-transport.service';
import { LibraryBook, BookStatus, BOOK_STATUS_OPTIONS, SUBJECT_CATEGORY_OPTIONS, LibraryRack, LibraryShelf, LibraryBookTransferResult } from '../library.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../core/toast/toast.service';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { LibraryBookTransferDialogComponent, LibraryBookTransferDialogData } from '../library-book-transfer-dialog/library-book-transfer-dialog.component';
import { LibraryBarcodePreviewDialogComponent, LibraryBarcodePreviewDialogData } from '../library-barcode-preview-dialog/library-barcode-preview-dialog.component';
import { LibraryItemHistoryDialogComponent, LibraryItemHistoryDialogData } from '../library-item-history-dialog/library-item-history-dialog.component';
import { ExportButtonComponent, ExportFormat } from '../../../shared/export-button';
import { ColumnPickerState, CmsColumnPickerComponent } from '../../../shared/column-picker';
import { ColumnResizeDirective, CmsWrapTextToggleComponent } from '../../../shared/column-resize';
import { CmsTourButtonComponent } from '../../../shared/tour/tour-button.component';
import { TourService } from '../../../shared/tour/tour.service';
import { LIBRARY_BOOK_LIST_TOUR, LIBRARY_BOOK_LIST_FLOW_MAP } from '../../../shared/tour/tours/library.tours';


@Component({
  selector: 'app-library-book-list',
  standalone: true,
  imports: [
    RouterLink, FormsModule, DatePipe,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatDialogModule, MatButtonModule, MatCheckboxModule, MatIconModule, MatTooltipModule,
    MatSelectModule, MatInputModule, MatFormFieldModule,
    CmsRowActionButtonComponent, CmsEmptyStateComponent, ExportButtonComponent, CmsColumnPickerComponent, ColumnResizeDirective, CmsWrapTextToggleComponent,
    LibraryBarcodePreviewDialogComponent, LibraryItemHistoryDialogComponent, LibraryBookTransferDialogComponent,
    CmsTourButtonComponent,
  ],
  templateUrl: './library-book-list.component.html',
  styleUrl: './library-book-list.component.scss',
})
export class LibraryBookListComponent implements OnInit, OnDestroy {
  private readonly libraryService = inject(LibraryService);
  private readonly printTransport = inject(LibraryPrintTransportService);
  private readonly router         = inject(Router);
  private readonly toast          = inject(ToastService);
  private readonly dialog         = inject(MatDialog);
  private readonly tourService    = inject(TourService);
  protected readonly permissions  = inject(PermissionService);

  private readonly destroy$     = new Subject<void>();
  private readonly searchSubject = new Subject<string>();
  private _paginator?: MatPaginator;
  private _paginatorSub?: Subscription;

  @ViewChild(MatTable) private _matTable?: MatTable<unknown>;
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

  protected readonly colState = new ColumnPickerState({
    storageKey: 'library-book-columns',
    columns: [
      { key: 'select',            label: 'Select',           mandatory: true, pinnable: false },
      { key: 'accessionNumber',   label: 'Acc. No.' },
      { key: 'title',             label: 'Title',            mandatory: true },
      { key: 'authors',           label: 'Author(s)' },
      { key: 'publisher',         label: 'Publisher' },
      { key: 'shelf',             label: 'Shelf' },
      { key: 'callNumber',        label: 'Call No.' },
      { key: 'status',            label: 'Status' },
      { key: 'entryDate',         label: 'Entry Date' },
      { key: 'isbn',              label: 'ISBN' },
      { key: 'edition',           label: 'Edition' },
      { key: 'yearOfPublication', label: 'Year' },
      { key: 'collation',         label: 'Collation' },
      { key: 'series',            label: 'Series' },
      { key: 'subjectCategory',   label: 'Subject' },
      { key: 'vendorDonorName',   label: 'Source / Vendor' },
      { key: 'billNumber',        label: 'Bill No. & Date' },
      { key: 'priceRs',           label: 'Price (₹)' },
      { key: 'remarks',           label: 'Remarks' },
      { key: 'actions',           label: 'Actions',          mandatory: true, pinnable: false },
    ],
    defaults: ['select', 'accessionNumber', 'title', 'authors', 'publisher', 'shelf', 'callNumber', 'status', 'actions'],
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());
  protected readonly dataSource    = new MatTableDataSource<LibraryBook>([]);
  protected readonly loading       = signal(false);
  protected readonly exporting     = signal(false);
  protected readonly searchValue   = signal('');
  protected readonly statusFilter  = signal<BookStatus | null>(null);
  protected readonly categoryFilter = signal<string | null>(null);
  protected readonly rackFilter    = signal<number | null>(null);
  protected readonly shelfFilter   = signal<number | null>(null);
  protected readonly racks         = signal<LibraryRack[]>([]);
  protected readonly shelves       = signal<LibraryShelf[]>([]);
  protected readonly statusOptions  = BOOK_STATUS_OPTIONS;
  protected readonly categoryOptions = SUBJECT_CATEGORY_OPTIONS;
  protected readonly canManage      = computed(() => this.permissions.hasAny('LIBRARY_CATALOGUE_MANAGE'));
  protected readonly canImport      = computed(() => this.permissions.hasAny('LIBRARY_IMPORT'));
  protected readonly canExport      = computed(() => this.permissions.hasAny('LIBRARY_CATALOGUE_EXPORT'));
  protected readonly canTransfer    = computed(() => this.permissions.hasAny('LIBRARY_TRANSFER'));
  protected readonly canPrintBarcode = computed(() => this.permissions.hasAny('LIBRARY_CATALOGUE_PRINT_BARCODE'));
  protected readonly printingLabels = signal(false);
  protected readonly canViewHistory = computed(() => this.permissions.hasAny('LIBRARY_CATALOGUE_VIEW_HISTORY'));

  protected readonly barcodeTarget  = signal<LibraryBarcodePreviewDialogData | null>(null);
  protected readonly historyTarget  = signal<LibraryItemHistoryDialogData | null>(null);
  protected readonly transferTarget = signal<LibraryBookTransferDialogData | null>(null);
  protected readonly hasActiveFilters = computed(() =>
    this.statusFilter() !== null || this.categoryFilter() !== null
    || this.rackFilter() !== null || this.shelfFilter() !== null || this.searchValue().length > 0);

  protected readonly selection = new SelectionModel<LibraryBook>(true, []);

  protected totalElements  = 0;
  protected currentPage    = 0;
  protected currentPageSize = 25;
  protected sortActive     = 'title';
  protected sortDirection: 'asc' | 'desc' = 'asc';


  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }
  ngOnInit(): void {
    this.tourService.register('library-book-list', LIBRARY_BOOK_LIST_TOUR);
    this.tourService.registerFlowMap('library-book-list', LIBRARY_BOOK_LIST_FLOW_MAP);
    this.searchSubject.pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => { this.currentPage = 0; this.loadPage(); });
    this.loadPage();
    this.libraryService.getRacks(undefined, true).subscribe({ next: (racks) => this.racks.set(racks) });
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

  protected onRackFilterChange(): void {
    this.shelfFilter.set(null);
    this.shelves.set([]);
    const rackId = this.rackFilter();
    if (rackId) {
      this.libraryService.getShelves(rackId, undefined, true).subscribe({ next: (shelves) => this.shelves.set(shelves) });
    }
    this.onFilterChange();
  }

  protected clearFilters(): void {
    this.searchValue.set(''); this.statusFilter.set(null); this.categoryFilter.set(null);
    this.rackFilter.set(null); this.shelfFilter.set(null); this.shelves.set([]);
    this.currentPage = 0; this.loadPage();
  }

  protected onSortChange(sort: Sort): void {
    this.sortActive = sort.active;
    this.sortDirection = sort.direction as 'asc' | 'desc';
    this.currentPage = 0;
    this.loadPage();
  }

  protected onExport(format: ExportFormat): void {
    if (this.exporting()) return;
    if (this.totalElements === 0) {
      this.toast.error('No data available to export.');
      return;
    }
    this.exporting.set(true);
    this.libraryService.exportBooks(format, {
      search: this.searchValue() || undefined,
      status: this.statusFilter(),
      category: this.categoryFilter(),
      rackId: this.rackFilter(),
      shelfId: this.shelfFilter(),
      sort: this.sortActive,
      direction: this.sortDirection,
      columns: this.colState.visibleColumns(),
    }).subscribe({
      next: blob => {
        const ext = format === 'pdf' ? 'pdf' : 'xlsx';
        const filename = `book-catalogue-${new Date().toISOString().slice(0, 10)}.${ext}`;
        const url = URL.createObjectURL(blob);
        const a = Object.assign(document.createElement('a'), { href: url, download: filename });
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        this.exporting.set(false);
      },
      error: () => {
        this.toast.error('Export failed. Please try again.');
        this.exporting.set(false);
      },
    });
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

  // ── Selection ────────────────────────────────────────────────

  protected isAllSelected(): boolean {
    return this.selection.selected.length === this.dataSource.data.length && this.dataSource.data.length > 0;
  }

  protected toggleAll(): void {
    if (this.isAllSelected()) this.selection.clear();
    else this.dataSource.data.forEach(row => this.selection.select(row));
  }

  // ── Transfer ─────────────────────────────────────────────────

  protected transferBook(book: LibraryBook): void {
    this.transferTarget.set({ books: [{ id: book.id, title: book.title, accessionNumber: book.accessionNumber }] });
  }

  protected transferSelected(): void {
    const books = this.selection.selected.map(b => ({ id: b.id, title: b.title, accessionNumber: b.accessionNumber }));
    this.transferTarget.set({ books });
  }

  protected onTransferClosed(result: LibraryBookTransferResult | undefined): void {
    this.transferTarget.set(null);
    if (result) {
      this.toast.success('Transfer complete');
      this.selection.clear();
      this.loadPage();
    }
  }

  // ── History ──────────────────────────────────────────────────

  protected viewHistory(book: LibraryBook): void {
    this.historyTarget.set({ itemType: 'BOOK', item: book });
  }

  // ── Barcode labels ───────────────────────────────────────────

  protected printBarcode(book: LibraryBook): void {
    this.barcodeTarget.set({ itemType: 'BOOK', id: book.id, title: book.title, code: book.barcode ?? book.accessionNumber });
  }

  protected printSelectedLabels(): void {
    if (this.printingLabels()) return;
    this.printingLabels.set(true);
    const ids = this.selection.selected.map(b => b.id);

    this.printTransport.getPrinterMode().subscribe(mode => {
      if (mode === 'BROWSER') {
        this.libraryService.printBookBarcodeLabels({ ids }).subscribe({
          next: blob => {
            const url = URL.createObjectURL(blob);
            window.open(url, '_blank');
            window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
            this.printingLabels.set(false);
          },
          error: () => {
            this.toast.error('Failed to generate barcode labels');
            this.printingLabels.set(false);
          },
        });
        return;
      }

      this.printTransport.sendBatch('BOOK', ids, mode).subscribe({
        next: result => {
          this.printingLabels.set(false);
          if (result.success) this.toast.success('Sent to printer');
          else this.toast.error(result.message ?? 'Failed to send to printer');
        },
        error: () => {
          this.printingLabels.set(false);
          this.toast.error('Failed to send to printer');
        },
      });
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    this.libraryService.getBooksPage({
      search: this.searchValue() || undefined,
      status: this.statusFilter(),
      category: this.categoryFilter(),
      rackId: this.rackFilter(),
      shelfId: this.shelfFilter(),
      page: this.currentPage, size: this.currentPageSize,
      sort: this.sortActive, direction: this.sortDirection,
    }).subscribe({
      next: page => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        this.selection.clear();
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
