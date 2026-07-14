import { Component, computed, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { LibraryService } from '../library.service';
import { LibraryFineDetail, FineStatus, LibraryMemberType, FINE_STATUS_OPTIONS } from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ExportButtonComponent, ExportFormat } from '../../../shared/export-button';
import { ColumnPickerState, CmsColumnPickerComponent } from '../../../shared/column-picker';

@Component({
  selector: 'app-library-fines',
  standalone: true,
  imports: [
    FormsModule, DatePipe, DecimalPipe,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatDialogModule, MatButtonModule, MatIconModule, MatTooltipModule,
    CmsRowActionButtonComponent, CmsTypeBadgeComponent, CmsEmptyStateComponent, ExportButtonComponent,
    CmsColumnPickerComponent,
  ],
  templateUrl: './library-fines.component.html',
  styleUrl: './library-fines.component.scss',
})
export class LibraryFinesComponent implements OnInit, OnDestroy {
  private readonly libraryService = inject(LibraryService);
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
    storageKey: 'library-fines-columns',
    columns: [
      { key: 'accessionNumber', label: 'Acc. No.' },
      { key: 'itemTitle',       label: 'Title' },
      { key: 'memberName',      label: 'Member' },
      { key: 'overdueDays',     label: 'Overdue Days' },
      { key: 'totalFine',       label: 'Fine' },
      { key: 'status',          label: 'Status' },
      { key: 'resolvedBy',      label: 'Resolved By' },
      { key: 'actions',         label: 'Actions', mandatory: true, pinnable: false },
    ],
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());
  protected readonly dataSource    = new MatTableDataSource<LibraryFineDetail>([]);
  protected readonly loading       = signal(false);
  protected readonly exporting     = signal(false);
  protected readonly searchValue   = signal('');
  protected readonly statusFilter  = signal<FineStatus | null>(null);
  protected readonly memberFilter  = signal<LibraryMemberType | null>(null);
  protected readonly statusOptions = FINE_STATUS_OPTIONS;
  protected readonly canExport     = computed(() => this.permissions.hasAny('LIBRARY_FINE_EXPORT'));
  protected readonly hasActiveFilters = computed(() =>
    this.statusFilter() !== null || this.memberFilter() !== null || this.searchValue().length > 0);

  protected totalElements  = 0;
  protected currentPage    = 0;
  protected currentPageSize = 25;
  protected sortActive     = 'createdAt';
  protected sortDirection: 'asc' | 'desc' = 'desc';

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
    this.searchValue.set(''); this.statusFilter.set(null); this.memberFilter.set(null);
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
    this.libraryService.exportFines(format, {
      search: this.searchValue() || undefined,
      status: this.statusFilter(),
      memberType: this.memberFilter(),
      sort: this.sortActive,
      direction: this.sortDirection,
    }).subscribe({
      next: blob => {
        const ext = format === 'pdf' ? 'pdf' : 'xlsx';
        const filename = `fine-register-${new Date().toISOString().slice(0, 10)}.${ext}`;
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

  protected statusLabel(status: FineStatus): string {
    return FINE_STATUS_OPTIONS.find(o => o.value === status)?.label ?? status;
  }

  protected statusClass(status: FineStatus): string {
    return FINE_STATUS_OPTIONS.find(o => o.value === status)?.colorClass ?? '';
  }

  protected resolvedBy(fine: LibraryFineDetail): string {
    if (fine.status === 'WAIVED')    return fine.waivedBy    ? `Waived by ${fine.waivedBy}` : 'Waived';
    if (fine.status === 'COLLECTED') return fine.collectedAt ? `Collected ${new DatePipe('en-IN').transform(fine.collectedAt, 'dd MMM yyyy')}` : 'Collected';
    return '—';
  }

  protected confirmWaive(fine: LibraryFineDetail): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Waive Fine', message: `Waive ₹${fine.totalFine} fine for "${fine.itemTitle}"?\nMember: ${fine.memberName}${fine.memberCode ? ' (' + fine.memberCode + ')' : ''}`, confirmText: 'Waive Fine', cancelText: 'Cancel' },
    }).afterClosed().subscribe(confirmed => { if (confirmed) this.performWaive(fine); });
  }

  protected confirmCollect(fine: LibraryFineDetail): void {
    const label = fine.itemType === 'BOOK' ? 'Book' : 'Journal';
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Collect Fine', message: `Mark ₹${fine.totalFine} fine as collected from ${fine.memberName}?\n${label}: "${fine.itemTitle}" (${fine.accessionNumber})`, confirmText: 'Mark Collected', cancelText: 'Cancel' },
    }).afterClosed().subscribe(confirmed => { if (confirmed) this.performCollect(fine); });
  }

  private performWaive(fine: LibraryFineDetail): void {
    this.libraryService.waiveFine(fine.id).subscribe({
      next: updated => { this.toast.success(`Fine of ₹${updated.totalFine} waived for ${updated.memberName}.`); this.loadPage(); },
      error: err => this.toast.error(err?.error?.message ?? 'Failed to waive fine'),
    });
  }

  private performCollect(fine: LibraryFineDetail): void {
    this.libraryService.collectFine(fine.id).subscribe({
      next: updated => { this.toast.success(`₹${updated.totalFine} collected from ${updated.memberName}.`); this.loadPage(); },
      error: err => this.toast.error(err?.error?.message ?? 'Failed to collect fine'),
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    this.libraryService.getFinesPage({
      search: this.searchValue() || undefined,
      status: this.statusFilter(),
      memberType: this.memberFilter(),
      page: this.currentPage, size: this.currentPageSize,
      sort: this.sortActive, direction: this.sortDirection,
    }).subscribe({
      next: page => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        if (this._paginator) { this._paginator.length = page.totalElements; this._paginator.pageIndex = page.number; }
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load fines'); this.loading.set(false); },
    });
  }
}
