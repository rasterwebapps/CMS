import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LibraryService } from '../library.service';
import {
  LibraryIssue,
  IssueStatus,
  LibraryMemberType,
  ISSUE_STATUS_OPTIONS,
} from '../library.model';
import { ToastService } from '../../../core/toast/toast.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { CmsEmptyStateComponent } from '../../../shared/empty-state/empty-state.component';

@Component({
  selector: 'app-library-issue-list',
  standalone: true,
  imports: [
    RouterLink,
    DatePipe,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    CmsEmptyStateComponent,
  ],
  templateUrl: './library-issue-list.component.html',
  styleUrl: './library-issue-list.component.scss',
})
export class LibraryIssueListComponent implements OnInit {
  private readonly libraryService = inject(LibraryService);
  private readonly router         = inject(Router);
  private readonly toast          = inject(ToastService);
  private readonly dialog         = inject(MatDialog);

  @ViewChild(MatPaginator) set paginator(v: MatPaginator) { if (v) this.dataSource.paginator = v; }
  @ViewChild(MatSort)      set sort(v: MatSort)           { if (v) this.dataSource.sort = v; }

  protected readonly displayedColumns = [
    'accessionNumber', 'bookTitle', 'memberName', 'issuedDate',
    'dueDate', 'returnedDate', 'status', 'fine', 'actions',
  ];

  protected readonly dataSource    = new MatTableDataSource<LibraryIssue>([]);
  protected readonly loading       = signal(false);
  protected readonly searchValue   = signal('');
  protected readonly statusFilter  = signal<IssueStatus | null>(null);
  protected readonly memberFilter  = signal<LibraryMemberType | null>(null);
  protected readonly overdueOnly   = signal(false);

  private readonly allIssues    = signal<LibraryIssue[]>([]);
  protected readonly statusOptions = ISSUE_STATUS_OPTIONS;

  protected readonly filteredIssues = computed(() => {
    const q       = this.searchValue().toLowerCase().trim();
    const status  = this.statusFilter();
    const member  = this.memberFilter();
    const overdue = this.overdueOnly();
    return this.allIssues().filter(i => {
      if (status  && i.status     !== status)  return false;
      if (member  && i.memberType !== member)  return false;
      if (overdue && i.status !== 'OVERDUE')   return false;
      if (q && !(
        i.accessionNumber.toLowerCase().includes(q) ||
        i.bookTitle.toLowerCase().includes(q) ||
        (i.studentName ?? '').toLowerCase().includes(q) ||
        (i.facultyName ?? '').toLowerCase().includes(q) ||
        (i.studentRollNumber ?? '').toLowerCase().includes(q) ||
        (i.facultyEmployeeCode ?? '').toLowerCase().includes(q)
      )) return false;
      return true;
    });
  });

  protected readonly totalIssued  = computed(() => this.allIssues().filter(i => i.status === 'ISSUED').length);
  protected readonly totalOverdue = computed(() => this.allIssues().filter(i => i.status === 'OVERDUE').length);
  protected readonly totalActive  = computed(() => this.allIssues().filter(i => i.status === 'ISSUED' || i.status === 'OVERDUE').length);

  protected readonly hasActiveFilters = computed(() =>
    this.statusFilter() !== null || this.memberFilter() !== null ||
    this.searchValue().length > 0 || this.overdueOnly(),
  );

  ngOnInit(): void {
    this.dataSource.sortingDataAccessor = (item, prop) =>
      String((item as unknown as Record<string, unknown>)[prop] ?? '').toLowerCase();
    this.loadIssues();
  }

  protected applyFilter(event: Event): void {
    this.searchValue.set((event.target as HTMLInputElement).value);
    this.syncTable();
  }

  protected clearFilters(): void {
    this.searchValue.set('');
    this.statusFilter.set(null);
    this.memberFilter.set(null);
    this.overdueOnly.set(false);
    this.syncTable();
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

    const fineMsg = isOverdue
      ? `\n\n⚠️ Overdue by ${overdueDays} day(s). A fine will be calculated on return.`
      : '';

    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Return Book',
        message: `Return "${issue.bookTitle}" (${issue.accessionNumber})\nBorrowed by: ${this.memberName(issue)}\nDue: ${issue.dueDate}${fineMsg}`,
        confirmText: 'Confirm Return',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) this.performReturn(issue);
    });
  }

  protected confirmRenew(issue: LibraryIssue): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Renew Book',
        message: `Renew "${issue.bookTitle}" for ${this.memberName(issue)}?\nRenewal ${issue.renewalCount + 1} will be applied.`,
        confirmText: 'Renew',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) this.performRenew(issue);
    });
  }

  protected syncTable(): void {
    this.dataSource.data = this.filteredIssues();
    this.dataSource.paginator?.firstPage();
  }

  private performReturn(issue: LibraryIssue): void {
    this.libraryService.returnBook(issue.id, {}).subscribe({
      next: returned => {
        const fineMsg = returned.fine
          ? ` Fine recorded: ₹${returned.fine.totalFine} (${returned.fine.overdueDays} overdue day(s)).`
          : '';
        this.toast.success(`Book returned successfully.${fineMsg}`);
        this.loadIssues();
      },
      error: err => this.toast.error(err?.error?.message ?? 'Failed to return book'),
    });
  }

  private performRenew(issue: LibraryIssue): void {
    this.libraryService.renewBook(issue.id, {}).subscribe({
      next: renewed => {
        this.toast.success(`Book renewed. New due date: ${renewed.dueDate}`);
        this.loadIssues();
      },
      error: err => this.toast.error(err?.error?.message ?? 'Failed to renew book'),
    });
  }

  private loadIssues(): void {
    this.loading.set(true);
    this.libraryService.getIssues().subscribe({
      next: issues => {
        this.allIssues.set(issues);
        this.syncTable();
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load issues');
        this.loading.set(false);
      },
    });
  }
}
