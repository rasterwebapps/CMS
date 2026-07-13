import { Component, computed, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource, MatTable } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { LibraryService } from '../library.service';
import { LibraryIssue, IssueStatus, LibraryMemberType, LibraryItemType, ISSUE_STATUS_OPTIONS } from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';
import { PermissionService } from '../../../core/permissions/permission.service';
import { ExportButtonComponent, ExportFormat } from '../../../shared/export-button';
import { CmsColumnPickerComponent, ColumnPickerState } from '../../../shared/column-picker';

@Component({
  selector: 'app-library-issue-list',
  standalone: true,
  imports: [
    RouterLink, DatePipe, FormsModule,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatDialogModule, MatButtonModule, MatIconModule, MatTooltipModule,
    CmsEmptyStateComponent, CmsRowActionButtonComponent, CmsTypeBadgeComponent, ExportButtonComponent,
    CmsColumnPickerComponent,
  ],
  templateUrl: './library-issue-list.component.html',
  styleUrl: './library-issue-list.component.scss',
})
export class LibraryIssueListComponent implements OnInit, OnDestroy {
  private readonly libraryService = inject(LibraryService);
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
    columns: [
      { key: 'accessionNumber', label: 'Acc. No.' },
      { key: 'itemTitle',       label: 'Item',     mandatory: true },
      { key: 'itemType',        label: 'Type' },
      { key: 'memberName',      label: 'Member',   mandatory: true },
      { key: 'memberType',      label: 'Role' },
      { key: 'issuedDate',      label: 'Issued' },
      { key: 'dueDate',         label: 'Due' },
      { key: 'returnedDate',    label: 'Returned' },
      { key: 'status',          label: 'Status' },
      { key: 'fine',            label: 'Fine' },
      { key: 'actions',         label: 'Actions',  mandatory: true, pinnable: false },
    ],
    storageKey: 'library-issue-cols-v1',
  });
  protected readonly displayedColumns = computed(() => this.colState.visibleColumns());
  protected readonly dataSource    = new MatTableDataSource<LibraryIssue>([]);
  protected readonly loading       = signal(false);
  protected readonly exporting     = signal(false);
  protected readonly searchValue   = signal('');
  protected readonly statusFilter  = signal<IssueStatus | null>(null);
  protected readonly memberFilter  = signal<LibraryMemberType | null>(null);
  protected readonly itemTypeFilter = signal<LibraryItemType | null>(null);
  protected readonly statusOptions = ISSUE_STATUS_OPTIONS;
  protected readonly canExport     = computed(() => this.permissions.hasAny('LIBRARY_ISSUE_EXPORT'));
  protected readonly canManageIssues = computed(() => this.permissions.hasAny('LIBRARY_ISSUE_MANAGE'));
  protected readonly hasActiveFilters = computed(() =>
    this.statusFilter() !== null || this.memberFilter() !== null
    || this.itemTypeFilter() !== null || this.searchValue().length > 0);

  protected readonly scanCode      = signal('');
  protected readonly scanning      = signal(false);

  protected totalElements  = 0;
  protected currentPage    = 0;
  protected currentPageSize = 25;
  protected sortActive     = 'issuedDate';
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
    this.itemTypeFilter.set(null);
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
    this.libraryService.exportIssues(format, {
      search: this.searchValue() || undefined,
      status: this.statusFilter(),
      memberType: this.memberFilter(),
      itemType: this.itemTypeFilter(),
    }).subscribe({
      next: blob => {
        const ext = format === 'pdf' ? 'pdf' : 'xlsx';
        const filename = `issue-register-${new Date().toISOString().slice(0, 10)}.${ext}`;
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

  protected statusLabel(status: IssueStatus): string {
    return ISSUE_STATUS_OPTIONS.find(o => o.value === status)?.label ?? status;
  }

  protected statusClass(status: IssueStatus): string {
    return ISSUE_STATUS_OPTIONS.find(o => o.value === status)?.colorClass ?? '';
  }

  protected memberName(issue: LibraryIssue): string {
    return issue.memberType === 'STUDENT'
      ? `${issue.studentName ?? '—'} (${issue.studentRollNumber ?? ''})`
      : `${issue.facultyName ?? '—'} (${issue.facultyEmployeeCode ?? ''})`;
  }

  protected isActive(issue: LibraryIssue): boolean {
    return issue.status === 'ISSUED' || issue.status === 'OVERDUE';
  }

  // ── Scan to return ───────────────────────────────────────────

  protected scanToReturn(): void {
    const code = this.scanCode().trim();
    if (!code || this.scanning()) return;
    this.scanning.set(true);
    this.libraryService.lookupActiveIssueByCode(code).subscribe({
      next: issue => {
        this.scanning.set(false);
        this.scanCode.set('');
        this.confirmReturn(issue);
      },
      error: err => {
        this.scanning.set(false);
        this.toast.error(err?.error?.message ?? `No active issue found for "${code}"`);
      },
    });
  }

  protected confirmReturn(issue: LibraryIssue): void {
    const today = new Date();
    const due   = new Date(issue.dueDate);
    const isOverdue = today > due;
    const overdueDays = isOverdue ? Math.floor((today.getTime() - due.getTime()) / 86400000) : 0;
    const fineMsg = isOverdue ? `\n\n⚠️ Overdue by ${overdueDays} day(s). A fine will be calculated on return.` : '';
    const label = issue.itemType === 'BOOK' ? 'Book' : 'Journal';
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: `Return ${label}`, message: `Return "${issue.itemTitle}" (${issue.accessionNumber})\nBorrowed by: ${this.memberName(issue)}\nDue: ${issue.dueDate}${fineMsg}`, confirmText: 'Confirm Return', cancelText: 'Cancel' },
    }).afterClosed().subscribe(confirmed => { if (confirmed) this.performReturn(issue); });
  }

  protected confirmRenew(issue: LibraryIssue): void {
    const label = issue.itemType === 'BOOK' ? 'Book' : 'Journal';
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: `Renew ${label}`, message: `Renew "${issue.itemTitle}" for ${this.memberName(issue)}?\nRenewal ${issue.renewalCount + 1} will be applied.`, confirmText: 'Renew', cancelText: 'Cancel' },
    }).afterClosed().subscribe(confirmed => { if (confirmed) this.performRenew(issue); });
  }

  private performReturn(issue: LibraryIssue): void {
    const label = issue.itemType === 'BOOK' ? 'Book' : 'Journal';
    this.libraryService.returnBook(issue.id, {}).subscribe({
      next: returned => {
        const fineMsg = returned.fine ? ` Fine recorded: ₹${returned.fine.totalFine} (${returned.fine.overdueDays} overdue day(s)).` : '';
        this.toast.success(`${label} returned successfully.${fineMsg}`);
        this.loadPage();
      },
      error: err => this.toast.error(err?.error?.message ?? `Failed to return ${label.toLowerCase()}`),
    });
  }

  private performRenew(issue: LibraryIssue): void {
    const label = issue.itemType === 'BOOK' ? 'Book' : 'Journal';
    this.libraryService.renewBook(issue.id, {}).subscribe({
      next: renewed => { this.toast.success(`${label} renewed. New due date: ${renewed.dueDate}`); this.loadPage(); },
      error: err => this.toast.error(err?.error?.message ?? `Failed to renew ${label.toLowerCase()}`),
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    this.libraryService.getIssuesPage({
      search: this.searchValue() || undefined,
      status: this.statusFilter(),
      memberType: this.memberFilter(),
      itemType: this.itemTypeFilter(),
      page: this.currentPage, size: this.currentPageSize,
      sort: this.sortActive, direction: this.sortDirection,
    }).subscribe({
      next: page => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        if (this._paginator) { this._paginator.length = page.totalElements; this._paginator.pageIndex = page.number; }
        this.loading.set(false);
      },
      error: () => { this.toast.error('Failed to load issues'); this.loading.set(false); },
    });
  }
}
