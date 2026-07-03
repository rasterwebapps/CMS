import { Component, computed, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { LibraryService } from '../library.service';
import { LibraryIssue, IssueStatus, LibraryMemberType, ISSUE_STATUS_OPTIONS } from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { CmsRowActionButtonComponent } from '../../../shared/row-action-button/row-action-button.component';
import { CmsTypeBadgeComponent } from '../../../shared/type-badge/type-badge.component';

@Component({
  selector: 'app-library-issue-list',
  standalone: true,
  imports: [
    RouterLink, DatePipe, FormsModule,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatDialogModule, MatButtonModule, MatIconModule, MatTooltipModule,
    CmsEmptyStateComponent, CmsRowActionButtonComponent, CmsTypeBadgeComponent,
  ],
  templateUrl: './library-issue-list.component.html',
  styleUrl: './library-issue-list.component.scss',
})
export class LibraryIssueListComponent implements OnInit, OnDestroy {
  private readonly libraryService = inject(LibraryService);
  private readonly router         = inject(Router);
  private readonly toast          = inject(ToastService);
  private readonly dialog         = inject(MatDialog);

  private readonly destroy$      = new Subject<void>();
  private readonly searchSubject  = new Subject<string>();
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
    'accessionNumber', 'bookTitle', 'memberName', 'issuedDate',
    'dueDate', 'returnedDate', 'status', 'fine', 'actions',
  ];
  protected readonly dataSource    = new MatTableDataSource<LibraryIssue>([]);
  protected readonly loading       = signal(false);
  protected readonly searchValue   = signal('');
  protected readonly statusFilter  = signal<IssueStatus | null>(null);
  protected readonly memberFilter  = signal<LibraryMemberType | null>(null);
  protected readonly statusOptions = ISSUE_STATUS_OPTIONS;
  protected readonly hasActiveFilters = computed(() =>
    this.statusFilter() !== null || this.memberFilter() !== null || this.searchValue().length > 0);

  protected totalElements  = 0;
  protected currentPage    = 0;
  protected currentPageSize = 25;
  protected sortActive     = 'issuedDate';
  protected sortDirection: 'asc' | 'desc' = 'desc';

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

  protected confirmReturn(issue: LibraryIssue): void {
    const today = new Date();
    const due   = new Date(issue.dueDate);
    const isOverdue = today > due;
    const overdueDays = isOverdue ? Math.floor((today.getTime() - due.getTime()) / 86400000) : 0;
    const fineMsg = isOverdue ? `\n\n⚠️ Overdue by ${overdueDays} day(s). A fine will be calculated on return.` : '';
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Return Book', message: `Return "${issue.bookTitle}" (${issue.accessionNumber})\nBorrowed by: ${this.memberName(issue)}\nDue: ${issue.dueDate}${fineMsg}`, confirmText: 'Confirm Return', cancelText: 'Cancel' },
    }).afterClosed().subscribe(confirmed => { if (confirmed) this.performReturn(issue); });
  }

  protected confirmRenew(issue: LibraryIssue): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Renew Book', message: `Renew "${issue.bookTitle}" for ${this.memberName(issue)}?\nRenewal ${issue.renewalCount + 1} will be applied.`, confirmText: 'Renew', cancelText: 'Cancel' },
    }).afterClosed().subscribe(confirmed => { if (confirmed) this.performRenew(issue); });
  }

  private performReturn(issue: LibraryIssue): void {
    this.libraryService.returnBook(issue.id, {}).subscribe({
      next: returned => {
        const fineMsg = returned.fine ? ` Fine recorded: ₹${returned.fine.totalFine} (${returned.fine.overdueDays} overdue day(s)).` : '';
        this.toast.success(`Book returned successfully.${fineMsg}`);
        this.loadPage();
      },
      error: err => this.toast.error(err?.error?.message ?? 'Failed to return book'),
    });
  }

  private performRenew(issue: LibraryIssue): void {
    this.libraryService.renewBook(issue.id, {}).subscribe({
      next: renewed => { this.toast.success(`Book renewed. New due date: ${renewed.dueDate}`); this.loadPage(); },
      error: err => this.toast.error(err?.error?.message ?? 'Failed to renew book'),
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    this.libraryService.getIssuesPage({
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
      error: () => { this.toast.error('Failed to load issues'); this.loading.set(false); },
    });
  }
}
