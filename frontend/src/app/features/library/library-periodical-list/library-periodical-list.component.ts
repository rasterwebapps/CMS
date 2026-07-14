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
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { LibraryService } from '../library.service';
import { LibraryPrintTransportService } from '../library-print-transport.service';
import { LibraryPeriodical, JournalType, SubscriptionStatus, JOURNAL_TYPE_OPTIONS, SUBSCRIPTION_STATUS_OPTIONS } from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { CmsStatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ExportButtonComponent, ExportFormat } from '../../../shared/export-button';
import { ColumnPickerState, CmsColumnPickerComponent } from '../../../shared/column-picker';
import { LibraryBarcodePreviewDialogComponent, LibraryBarcodePreviewDialogData } from '../library-barcode-preview-dialog/library-barcode-preview-dialog.component';
import { LibraryItemHistoryDialogComponent, LibraryItemHistoryDialogData } from '../library-item-history-dialog/library-item-history-dialog.component';


@Component({
  selector: 'app-library-periodical-list',
  standalone: true,
  imports: [
    RouterLink, DatePipe, FormsModule,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatDialogModule, MatButtonModule, MatCheckboxModule, MatIconModule, MatTooltipModule,
    CmsEmptyStateComponent, CmsRowActionButtonComponent,
    CmsTypeBadgeComponent, CmsStatusBadgeComponent, ExportButtonComponent, CmsColumnPickerComponent,
    LibraryBarcodePreviewDialogComponent, LibraryItemHistoryDialogComponent,
  ],
  templateUrl: './library-periodical-list.component.html',
  styleUrl:    './library-periodical-list.component.scss',
})
export class LibraryPeriodicalListComponent implements OnInit, OnDestroy {
  private readonly libraryService = inject(LibraryService);
  private readonly printTransport = inject(LibraryPrintTransportService);
  private readonly router         = inject(Router);
  private readonly toast          = inject(ToastService);
  private readonly dialog         = inject(MatDialog);
  protected readonly permissions  = inject(PermissionService);

  private readonly destroy$      = new Subject<void>();
  private readonly searchSubject  = new Subject<string>();
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
    storageKey: 'library-periodical-columns',
    columns: [
      { key: 'select',             label: 'Select',      mandatory: true, pinnable: false },
      { key: 'accessionNumber',    label: 'Acc. No.' },
      { key: 'journalName',        label: 'Journal' },
      { key: 'journalType',        label: 'Type' },
      { key: 'volumeIssue',        label: 'Vol./Issue' },
      { key: 'year',               label: 'Year' },
      { key: 'subscriptionStatus', label: 'Status' },
      { key: 'receivedDate',       label: 'Received' },
      { key: 'actions',            label: 'Actions', mandatory: true, pinnable: false },
    ],
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());
  protected readonly dataSource    = new MatTableDataSource<LibraryPeriodical>([]);
  protected readonly loading       = signal(false);
  protected readonly exporting     = signal(false);
  protected readonly searchValue   = signal('');
  protected readonly typeFilter    = signal<JournalType | null>(null);
  protected readonly statusFilter  = signal<SubscriptionStatus | null>(null);
  protected readonly typeOptions   = JOURNAL_TYPE_OPTIONS;
  protected readonly statusOptions = SUBSCRIPTION_STATUS_OPTIONS;
  protected readonly canManage     = computed(() => this.permissions.hasAny('LIBRARY_PERIODICAL_MANAGE'));
  protected readonly canExport     = computed(() => this.permissions.hasAny('LIBRARY_PERIODICAL_EXPORT'));
  protected readonly canImport     = computed(() => this.permissions.hasAny('LIBRARY_PERIODICAL_IMPORT'));
  protected readonly canPrintBarcode = computed(() => this.permissions.hasAny('LIBRARY_PERIODICAL_PRINT_BARCODE'));
  protected readonly printingLabels = signal(false);
  protected readonly canViewHistory = computed(() => this.permissions.hasAny('LIBRARY_PERIODICAL_VIEW_HISTORY'));
  protected readonly hasActiveFilters = computed(() =>
    this.typeFilter() !== null || this.statusFilter() !== null || this.searchValue().length > 0);

  protected readonly barcodeTarget = signal<LibraryBarcodePreviewDialogData | null>(null);
  protected readonly historyTarget = signal<LibraryItemHistoryDialogData | null>(null);

  protected readonly selection = new SelectionModel<LibraryPeriodical>(true, []);

  protected totalElements  = 0;
  protected currentPage    = 0;
  protected currentPageSize = 25;
  protected sortActive     = 'journalName';
  protected sortDirection: 'asc' | 'desc' = 'asc';


  protected onPinChange(): void { this._matTable?.updateStickyColumnStyles(); }
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

  protected onExport(format: ExportFormat): void {
    if (this.exporting()) return;
    this.exporting.set(true);
    this.libraryService.exportPeriodicals(format, {
      search: this.searchValue() || undefined,
      subscriptionStatus: this.statusFilter(),
      journalType: this.typeFilter(),
      sort: this.sortActive,
      direction: this.sortDirection,
    }).subscribe({
      next: blob => {
        const ext = format === 'pdf' ? 'pdf' : 'xlsx';
        const filename = `journals-periodicals-${new Date().toISOString().slice(0, 10)}.${ext}`;
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

  // ── Selection ────────────────────────────────────────────────

  protected isAllSelected(): boolean {
    return this.selection.selected.length === this.dataSource.data.length && this.dataSource.data.length > 0;
  }

  protected toggleAll(): void {
    if (this.isAllSelected()) this.selection.clear();
    else this.dataSource.data.forEach(row => this.selection.select(row));
  }

  // ── History ──────────────────────────────────────────────────

  protected viewHistory(p: LibraryPeriodical): void {
    this.historyTarget.set({ itemType: 'JOURNAL', item: p });
  }

  // ── Barcode labels ───────────────────────────────────────────

  protected printBarcode(p: LibraryPeriodical): void {
    this.barcodeTarget.set({ itemType: 'JOURNAL', id: p.id, title: p.journalName, code: p.barcode ?? p.accessionNumber });
  }

  protected printSelectedLabels(): void {
    if (this.printingLabels()) return;
    this.printingLabels.set(true);
    const ids = this.selection.selected.map(p => p.id);

    this.printTransport.getPrinterMode().subscribe(mode => {
      if (mode === 'BROWSER') {
        this.libraryService.printPeriodicalBarcodeLabels({ ids }).subscribe({
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

      this.printTransport.sendBatch('JOURNAL', ids, mode).subscribe({
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
        this.selection.clear();
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
